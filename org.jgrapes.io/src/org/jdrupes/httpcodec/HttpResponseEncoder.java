/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jdrupes.httpcodec;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Stack;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpIntField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.util.ByteBufferOutputStream;

/**
 * @author Michael N. Lipp
 */
public class HttpResponseEncoder {

	private enum State { INITIAL, HEADERS, CHUNK_BODY, START_COLLECT_BODY,
		COLLECT_BODY, STREAM_COLLECTED, STREAM_BODY, DONE
	}

	private final ByteBuffer EMPTY_IN = ByteBuffer.allocate(0);
	
	private Stack<State> states = new Stack<>();
	private boolean closeAfterBody = false;
	private ByteBufferOutputStream outStream;
	private Writer writer;
	private HttpResponse response = null;
	private Iterator<HttpField<?>> headerIter = null;
	private int pendingLimit = 1024*1024;
	private long contentLength;
	private ByteBufferOutputStream pendingBodyData;

	/**
	 * Creates a new encoder.
	 */
	public HttpResponseEncoder() {
		outStream = new ByteBufferOutputStream();
		try {
			writer = new OutputStreamWriter(outStream, "ascii");
		} catch (UnsupportedEncodingException e) {
		}
		states.push(State.INITIAL);
	}

	/**
	 * Returns the limit for pending body bytes. If the protocol
	 * is HTTP/1.0 and the request has a body but no "Content-Length" 
	 * header, the only (unreliable) way to indicate the end of the
	 * body is to close the connection after all body bytes have
	 * been sent.
	 * <P>
	 * The encoder tries to calculate the content length by buffering
	 * the body data up to the "pending" limit. If the body is smaller
	 * than the limit, the response is set with the calculated content 
	 * length header, else the data is sent without such a header
	 * and the connection is closed.
	 * <P> 
	 * If the response protocol is HTTP/1.1 and there is no
	 * "content-length" header, chunked transfer encoding is used.
	 *
	 * @return the limit
	 */
	public int getPendingLimit() {
		return pendingLimit;
	}

	/**
	 * Sets the limit for the pending body bytes.
	 * 
	 * @param pendingLimit the limit to set
	 */
	public void setPendingLimit(int pendingLimit) {
		this.pendingLimit = pendingLimit;
	}

	/**
	 * Set a new HTTP response that is to be encoded.  
	 * 
	 * @param response the response
	 */
	public void encode (HttpResponse response) {
		if (states.peek() != State.INITIAL) {
			throw new IllegalStateException();
		}
		this.response = response;
	}

	/**
	 * Convenience method for invoking 
	 * {@link #encode(ByteBuffer, ByteBuffer)} with an empty
	 * {@code in} buffer.
	 * 
	 * @param out the buffer to which data is written
	 * @return the result
	 */
	public EncoderResult encode(ByteBuffer out) {
		return encode(EMPTY_IN, out);
	}

	/**
	 * Encodes a HTTP response.  
	 * 
	 * @param in the body data
	 * @param out the buffer to which data is written
	 * @return the result
	 */
	public EncoderResult encode (ByteBuffer in, ByteBuffer out) {
		outStream.assignBuffer(out);
		while (true) {
			if (out.remaining() == 0) {
				return EncoderResult.OVERFLOW;
			}
			switch (states.peek()) {
			case INITIAL:
				startResponse();
				break;
				
			case HEADERS:
				// If headers remain (new request or buffer was full) write them
				continueHeaders();
				break;
				
			case START_COLLECT_BODY:
				// Start collecting
				if (in == null) {
					// Empty body
					response.setHeader(
					        new HttpIntField(HttpField.CONTENT_LENGTH, 0));
					states.pop();
					states.push(State.HEADERS);
					break;
				}
				if (in.remaining() == 0) {
					// Has probably been invoked with a dummy buffer.
					return EncoderResult.PROCEED;
				}
				pendingBodyData = new ByteBufferOutputStream(in.capacity());
				states.pop();
				states.push(State.COLLECT_BODY);
				// fall through (no write occurred)
			case COLLECT_BODY:
				collectBody(in);
				break;
				
			case STREAM_COLLECTED:
				// Output collected body
				if (pendingBodyData.buffered() > 0) {
					pendingBodyData.assignBuffer(out);
					break;
				}
				states.pop();
				break;
				
			case STREAM_BODY:
				// Stream, i.e. simply copy from source to destination
				if (in == null) {
					// end of data
					states.pop();
				}
				// More data
				int chunkSize = in.remaining();
				if (!ByteBufferOutputStream.putAsMuchAsPossible(out, in)) {
					return EncoderResult.OVERFLOW; // Shortcut
				}
				// Everything written
				if (contentLength > 0) {
					// We have an expected content-length
					contentLength -= chunkSize;
					if (contentLength > 0) {
						// Expecting more
						return EncoderResult.UNDERFLOW;
					}
				}
				// Waiting for more data or end of data
				return EncoderResult.PROCEED;

			case CHUNK_BODY:
				// Send in data as chunk
				if (in == EMPTY_IN) {
					return EncoderResult.PROCEED;
				}
				EncoderResult result = writeChunk(in);
				if (result.isOverflow()) {
					return result;
				}
				break;
				
			case DONE:
				// Was called with in == null and everything is written
				states.pop();
				if (closeAfterBody) {
					return EncoderResult.SEND_CLOSE;
				}
				return EncoderResult.PROCEED;
				
			default:
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * Called when encode is invoked with a new response (initial state).
	 * Writes the status line, determines the body-mode and puts the state
	 * machine in output-headers mode, unless the body-mode is "collect body".
	 * 
	 * @param response
	 */
	private void startResponse() {
		try {
			outStream.clear();
			headerIter = null;
			// Complete content type
			HttpMediaTypeField contentType = (HttpMediaTypeField) response
			        .headers().get(HttpField.CONTENT_TYPE);
			String charset = null;
			if (contentType != null) {
				charset = contentType.getParameter("charset");
				if (charset == null) {
					charset = "utf-8";
					contentType.setParameter("charset", charset);
				}
			}

			// Write status line
			writer.write(response.getProtocol().toString());
			writer.write(" ");
			writer.write(Integer.toString(response.getStatusCode()));
			writer.write(" ");
			writer.write(response.getReasonPhrase());
			writer.write("\r\n");
		} catch (IOException e) {
			// Formally thrown by writer, cannot happen.
		}
		states.push(State.DONE);
		HttpStringListField conField = response
		        .getHeader(HttpStringListField.class, HttpField.CONNECTION);
		closeAfterBody = conField != null
		        && conField.containsIgnoreCase("close");
		if (!response.hasBody()) {
			states.push(State.HEADERS);
			return;
		}
		HttpIntField cl = response.getHeader(HttpIntField.class,
		        HttpField.CONTENT_LENGTH);
		contentLength = (cl == null ? -1 : cl.getValue());
		if (contentLength >= 0) {
			// Easiest: we have a content length, works always
			states.push(State.STREAM_BODY);
			states.push(State.HEADERS);
		} else if (response.getProtocol()
		        .compareTo(HttpProtocol.HTTP_1_0) > 0) {
			// At least 1.1, use chunks
			HttpStringListField transEnc = response.getHeader(
			        HttpStringListField.class, HttpField.TRANSFER_ENCODING);
			if (transEnc == null) {
				try {
					response.setHeader(new HttpStringListField(
					        HttpField.TRANSFER_ENCODING, "chunked"));
				} catch (ParseException e) {
				}
			} else {
				transEnc.remove("chunked");
				transEnc.add("chunked");
			}
			states.push(State.CHUNK_BODY);
			states.push(State.HEADERS);
		} else {
			// Bad: 1.0 and no content length.
			if (pendingLimit > 0) {
				// Try to calculate length by collecting the data
				contentLength = 0;
				states.push(State.START_COLLECT_BODY);
				return;
			}
			// May not calculate, use close
			states.push(State.STREAM_BODY);
			states.push(State.HEADERS);
			closeAfterBody = true;
		}
	}
	
	/**
	 * Outputs as many headers as fit in the current out buffer. If all
	 * headers are output, pops a state (thus proceeding with the selected
	 * body mode). Unless very small out buffers are used (or very 
	 * large headers occur), this is invoked only once. Therefore no
	 * attempt has been made to avoid the usage of temporary buffers in
	 * the header header stream (there may be a maximum overflow of one
	 * partial header).
	 */
	private void continueHeaders() {
		try {
			if (headerIter == null) {
				headerIter = response.headers().values().iterator();
			}
			while (true) {
				if (!headerIter.hasNext()) {
					writer.write("\r\n");
					writer.flush();
					states.pop();
					return;
				}
				HttpField<?> header = headerIter.next();
				writer.write(header.toString());
				writer.write("\r\n");
				writer.flush();
				if (outStream.remaining() <= 0) {
					return;
				}
			}
		} catch (IOException e) {
			// Formally thrown by writer, cannot happen.
		}
	}

	/**
	 * Handle the input as appropriate for the collect-body mode.
	 * 
	 * @param in
	 */
	private void collectBody(ByteBuffer in) {
		if (in == null) {
			// End of body, found content length!
			response.setHeader(new HttpIntField(HttpField.CONTENT_LENGTH,
					pendingBodyData.buffered()));
			states.pop();
			states.push(State.STREAM_COLLECTED);
			states.push(State.HEADERS);
			return;
		}
		if (pendingBodyData.buffered() + in.remaining() < pendingLimit) {
			// Space left, collect
			pendingBodyData.write(in);
			return; 
		}
		// No space left, output headers, collected and rest (and then close)
		states.pop();
		closeAfterBody = true;
		states.push(State.STREAM_BODY);
		states.push(State.STREAM_COLLECTED);
		states.push(State.HEADERS);
	}

	/**
	 * Handle the input as appropriate for the chunked-body mode.
	 * 
	 * @param in
	 * @return
	 */
	private EncoderResult writeChunk(ByteBuffer in) {
		try {
			if (in == null) {
				writer.write("0\r\n\r\n");
				writer.flush();
				states.pop();
				return EncoderResult.PROCEED;
			}
			// We may loose some bytes here, but else we need an elaborate
			// calculation
			if (outStream.remaining() < 11) {
				// max 8 digits chunk size + CRLF + 1 octet = 11
				return EncoderResult.OVERFLOW;
			}
			int length = Math.min(outStream.remaining() - 11, in.remaining());
			writer.write(Integer.toHexString(length));
			writer.write("\r\n");
			writer.flush();
			outStream.write(in, length);
		} catch (IOException e) {
			// Formally thrown by writer, cannot happen.
		}
		return in.remaining() > 0 
				? EncoderResult.OVERFLOW : EncoderResult.PROCEED;
	}

}
