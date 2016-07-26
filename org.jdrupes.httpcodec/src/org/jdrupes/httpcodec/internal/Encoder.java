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
package org.jdrupes.httpcodec.internal;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Stack;

import org.jdrupes.httpcodec.HttpCodec;
import org.jdrupes.httpcodec.fields.HttpContentLengthField;
import org.jdrupes.httpcodec.fields.HttpDateField;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpIntField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.util.ByteBufferOutputStream;
import org.jdrupes.httpcodec.util.ByteBufferUtils;

/**
 * @author Michael N. Lipp
 *
 */
public abstract class Encoder<T extends MessageHeader> extends HttpCodec {

	private enum State {
		// Main states
		INITIAL, DONE, CLOSED,
		// Sub states
		HEADERS, CHUNK_BODY, START_COLLECT_BODY, COLLECT_BODY, 
		STREAM_COLLECTED, STREAM_BODY
	}

	private final ByteBuffer EMPTY_IN = ByteBuffer.allocate(0);

	private Stack<State> states = new Stack<>();
	private boolean closeAfterBody = false;
	private ByteBufferOutputStream outStream;
	private Writer writer;
	private T messageHeader = null;
	private Iterator<HttpField<?>> headerIter = null;
	private int pendingLimit = 1024 * 1024;
	private long contentLength;
	private ByteBufferOutputStream pendingBodyData;

	/**
	 * Creates a new encoder.
	 */
	public Encoder() {
		outStream = new ByteBufferOutputStream();
		try {
			writer = new OutputStreamWriter(outStream, "ascii");
		} catch (UnsupportedEncodingException e) {
		}
		states.push(State.INITIAL);
	}

	/**
	 * Returns the limit for pending body bytes. If the protocol is HTTP/1.0 and
	 * the message has a body but no "Content-Length" header, the only
	 * (unreliable) way to indicate the end of the body is to close the
	 * connection after all body bytes have been sent.
	 * <P>
	 * The encoder tries to calculate the content length by buffering the body
	 * data up to the "pending" limit. If the body is smaller than the limit,
	 * the message is set with the calculated content length header, else the
	 * data is sent without such a header and the connection is closed.
	 * <P>
	 * If the response protocol is HTTP/1.1 and there is no "Content-Length"
	 * header, chunked transfer encoding is used.
	 *
	 * @return the limit
	 */
	public int getPendingLimit() {
		return pendingLimit;
	}

	/**
	 * Sets the limit for the pending body bytes.
	 * 
	 * @param pendingLimit
	 *            the limit to set
	 */
	public void setPendingLimit(int pendingLimit) {
		this.pendingLimit = pendingLimit;
	}

	/**
	 * Returns {@code true} if the encoder does not accept further input because
	 * the processed data indicated that the connection has been or is to be
	 * closed.
	 * 
	 * @return the result
	 */
	public boolean isClosed() {
		return states.peek() == State.CLOSED;
	}

	/**
	 * Writes the first line of the message (including the terminating CRLF).
	 * Must be provided by the derived class because the first line depends on
	 * whether a request or response is encoded.
	 * 
	 * @param messageHeader
	 *            the message header to encode (see
	 *            {@link #encode(MessageHeader)}
	 * @param writer
	 *            the Writer to use for writing
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void startMessage(T messageHeader, Writer writer)
	        throws IOException;

	/**
	 * Factory method to be implemented by derived classes that returns an
	 * encoder result as appropriate for the encoder.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 */
	protected abstract Encoder.Result newResult(boolean overflow,
	        boolean underflow);

	/**
	 * Set a new HTTP message that is to be encoded.
	 * 
	 * @param messageHeader
	 *            the response
	 */
	public void encode(T messageHeader) {
		if (states.peek() != State.INITIAL) {
			throw new IllegalStateException();
		}
		this.messageHeader = messageHeader;
	}

	/**
	 * Convenience method for invoking {@link #encode(ByteBuffer, ByteBuffer)}
	 * with an empty {@code in} buffer.
	 * 
	 * @param out
	 *            the buffer to which data is written
	 * @return the result
	 */
	public Result encode(ByteBuffer out) {
		return encode(EMPTY_IN, out);
	}

	/**
	 * Encodes a HTTP message.
	 * 
	 * @param in
	 *            the body data
	 * @param out
	 *            the buffer to which data is written
	 * @return the result
	 */
	public Result encode(ByteBuffer in, ByteBuffer out) {
		outStream.assignBuffer(out);
		Result result = newResult(false, false);
		while (true) {
			if (result.isOverflow() || result.isUnderflow()) {
				return result;
			}
			if (out.remaining() == 0) {
				return newResult(true, false);
			}
			switch (states.peek()) {
			case INITIAL:
				startMessage();
				break;

			case HEADERS:
				// If headers remain (new request or buffer was full) write them
				continueHeaders();
				break;

			case START_COLLECT_BODY:
				// Start collecting
				if (in == null) {
					// Empty body
					messageHeader.setField(new HttpContentLengthField(0));
					states.pop();
					states.push(State.HEADERS);
					break;
				}
				if (in.remaining() == 0) {
					// Has probably been invoked with a dummy buffer,
					// cannot be used to create pending body buffer.
					return newResult(false, true);
				}
				pendingBodyData = new ByteBufferOutputStream(in.capacity());
				states.pop();
				states.push(State.COLLECT_BODY);
				// fall through (no write occurred)
			case COLLECT_BODY:
				result = collectBody(in);
				break;

			case STREAM_COLLECTED:
				// Output collected body
				if (pendingBodyData.remaining() < 0) {
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
					break;
				}
				// More data
				if (!ByteBufferUtils.putAsMuchAsPossible(out, in)) {
					return newResult(true, false); // Shortcut
				}
				// Everything written, waiting for more data or end of data
				return newResult(false, true);

			case CHUNK_BODY:
				// Send in data as chunk
				result = writeChunk(in, out);
				break;

			case DONE:
				// Was called with in == null and everything is written
				states.pop();
				if (closeAfterBody) {
					states.push(State.CLOSED);
				} else {
					states.push(State.INITIAL);
				}
				return newResult(false, false);

			default:
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * Called during the initial state. Writes the status line, determines the
	 * body-mode and puts the state machine in output-headers mode, unless the
	 * body-mode is "collect body".
	 */
	private void startMessage() {
		// Make sure we have a Date, RFC 7231 7.1.1.2
		if (!messageHeader.fields().containsKey(HttpField.DATE)) {
			messageHeader.setField(new HttpDateField());
		}

		// Complete content type
		HttpMediaTypeField contentType = (HttpMediaTypeField) messageHeader
		        .fields().get(HttpField.CONTENT_TYPE);
		String charset = null;
		if (contentType != null) {
			charset = contentType.getParameter("charset");
			if (charset == null) {
				charset = "utf-8";
				contentType.setParameter("charset", charset);
			}
		}
		
		// Prepare encoder
		outStream.clear();
		headerIter = null;
		// Write request or status line
		try {
			startMessage(messageHeader, writer);
		} catch (IOException e) {
			// Formally thrown by writer, cannot happen.
		}
		// We'll eventually fall back to this state
		states.pop();
		states.push(State.DONE);
		// Get a default for closeAfterBody from the header fields
		HttpStringListField conField = messageHeader
		        .getField(HttpStringListField.class, HttpField.CONNECTION);
		closeAfterBody = conField != null
		        && conField.containsIgnoreCase("close");
		// If there's no body start outputting header fields
		if (!messageHeader.messageHasBody()) {
			states.push(State.HEADERS);
			return;
		}
		// Message has a body, find out how to handle it
		HttpIntField cl = messageHeader.getField(HttpIntField.class,
		        HttpField.CONTENT_LENGTH);
		contentLength = (cl == null ? -1 : cl.getValue());
		if (contentLength >= 0) {
			// Easiest: we have a content length, works always
			states.push(State.STREAM_BODY);
			states.push(State.HEADERS);
			return;
		} 
		if (messageHeader.getProtocol()
		        .compareTo(HttpProtocol.HTTP_1_0) > 0) {
			// At least 1.1, use chunks
			HttpStringListField transEnc = messageHeader.getField(
			        HttpStringListField.class, HttpField.TRANSFER_ENCODING);
			if (transEnc == null) {
				messageHeader.setField(new HttpStringListField(
				        HttpField.TRANSFER_ENCODING,
				        TransferCoding.CHUNKED.toString()));
			} else {
				transEnc.remove(TransferCoding.CHUNKED.toString());
				transEnc.add(TransferCoding.CHUNKED.toString());
			}
			states.push(State.CHUNK_BODY);
			states.push(State.HEADERS);
			return;
		}
		// Bad: 1.0 and no content length.
		if (pendingLimit > 0) {
			// Try to calculate length by collecting the data
			contentLength = 0;
			states.push(State.START_COLLECT_BODY);
			return;
		}
		// May not buffer, use close
		states.push(State.STREAM_BODY);
		states.push(State.HEADERS);
		closeAfterBody = true;
	}

	/**
	 * Outputs as many headers as fit in the current out buffer. If all headers
	 * are output, pops a state (thus proceeding with the selected body mode).
	 * Unless very small out buffers are used (or very large headers occur),
	 * this is invoked only once. Therefore no attempt has been made to avoid
	 * the usage of temporary buffers in the header header stream (there may be
	 * a maximum overflow of one partial header).
	 */
	private void continueHeaders() {
		try {
			if (headerIter == null) {
				headerIter = messageHeader.fields().values().iterator();
			}
			while (true) {
				if (!headerIter.hasNext()) {
					writer.write("\r\n");
					writer.flush();
					states.pop();
					return;
				}
				HttpField<?> header = headerIter.next();
				writer.write(header.asHeaderField());
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
	private Result collectBody(ByteBuffer in) {
		if (in == null) {
			// End of body, found content length!
			messageHeader.setField(new HttpContentLengthField(
			        pendingBodyData.bytesWritten()));
			states.pop();
			states.push(State.STREAM_COLLECTED);
			states.push(State.HEADERS);
			return newResult(false, false);
		}
		if (pendingBodyData.remaining() - in.remaining() > -pendingLimit) {
			// Space left, collect
			pendingBodyData.write(in);
			return newResult(false, true);
		}
		// No space left, output headers, collected and rest (and then close)
		states.pop();
		closeAfterBody = true;
		states.push(State.STREAM_BODY);
		states.push(State.STREAM_COLLECTED);
		states.push(State.HEADERS);
		return newResult(false, true);
	}

	/**
	 * Handle the input as appropriate for the chunked-body mode.
	 * 
	 * @param in
	 * @return
	 */
	private Result writeChunk(ByteBuffer in, ByteBuffer out) {
		try {
			if (in == null) {
				outStream.write("0\r\n\r\n".getBytes("ascii"));
				states.pop();
				return newResult(false, false);
			}
			// Don't write zero sized chunks
			if (!in.hasRemaining()) {
				return newResult(false, true);
			}
			// We may loose some bytes here, but else we need an elaborate
			// calculation
			if (outStream.remaining() < 13) {
				// max 8 digits chunk size + CRLF + 1 octet + CRLF = 13
				return newResult(true, false);
			}
			int length;
			if (outStream.remaining() - 13 < in.remaining()) {
				// Cast always works because in.remaining <= Integer.MAX_VALUE
				length = (int)(outStream.remaining() - 13);
			} else {
				length = in.remaining();
			}
			outStream.write(Integer.toHexString(length).getBytes("ascii"));
			outStream.write("\r\n".getBytes("ascii"));
			outStream.write(in, length);
			outStream.write("\r\n".getBytes("ascii"));
			outStream.flush();
		} catch (IOException e) {
			// Formally thrown by outStream, cannot happen.
		}
		return in.remaining() > 0 ? newResult(true, false)
		        : newResult(false, true);
	}

	public static class Result extends CodecResult {

		/**
		 * Creates a new result with the given values.
		 * 
		 * @param overflow
		 * @param underflow
		 * @param closeConnection
		 */
		public Result(boolean overflow, boolean underflow) {
			super(overflow, underflow);
		}

	}
}
