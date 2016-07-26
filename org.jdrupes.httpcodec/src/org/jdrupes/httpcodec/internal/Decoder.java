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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.HttpCodec;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.fields.HttpContentLengthField;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpListField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.util.ByteBufferUtils;
import org.jdrupes.httpcodec.util.DynamicByteArray;

/**
 * @author Michael N. Lipp
 *
 */
public abstract class Decoder<T extends MessageHeader> extends HttpCodec {

	final protected static String TOKEN = "[" + Pattern.quote(TOKEN_CHARS)
	        + "]+";
	final protected static String SP = "[ \\t]+";
	final protected static String HTTP_VERSION = "HTTP/\\d+\\.\\d";

	// RFC 7230 3.2, 3.2.4
	final protected static Pattern headerLinePatter = Pattern
	        .compile("^(" + TOKEN + "):(.*)$");
	
    private enum State {
    	// Main states
        AWAIT_MESSAGE_START, HEADER_LINE_RECEIVED, 
        COPY_UNTIL_CLOSED, CONTENT_RECEIVED, 
        CHUNK_START_RECEIVED, CHUNK_END_RECEIVED, CHUNK_TRAILER_LINE_RECEIVED,
        CLOSED, 
        // Sub states
        RECEIVE_LINE, AWAIT_LINE_END, COPY_SPECIFIED
    }

	protected enum BodyMode {
		NO_BODY, CHUNKED, LENGTH, UNTIL_CLOSE
	};
    
    private long maxHeaderLength = 4194304;

    private Stack<State> states = new Stack<>();
    private DynamicByteArray lineBuilder = new DynamicByteArray(8192);
    private String receivedLine;
    private String headerLine = null;
    protected HttpProtocol protocolVersion = HttpProtocol.HTTP_1_0;
    private long headerLength = 0;
    private T building;
    private T built;
    private long leftToRead = 0;

    /**
     * Creates a new decoder.
     */
    public Decoder() {
    	states.push(State.AWAIT_MESSAGE_START);
    	states.push(State.RECEIVE_LINE);
	}

	/**
	 * Sets the maximum size for the complete header. If the size is exceeded,
	 * a {@link ProtocolException} will be thrown. The default size is
	 * 4MB (4194304 Byte).
	 * 
	 * @param maxHeaderLength the maxHeaderLength to set
	 */
	public void setMaxHeaderLength(long maxHeaderLength) {
		this.maxHeaderLength = maxHeaderLength;
	}

    /**
     * Returns the maximum header length.
     * 
	 * @return the maxHeaderLength
	 */
	public long getMaxHeaderLength() {
		return maxHeaderLength;
	}

	/**
	 * Returns the message (header) if it has been received completely, else
	 * {@code null}.
	 * 
	 * @return the result
	 */
	public T getHeader() {
		return built;
	}
	
	/**
	 * Informs the derived class about the start of a new message.
	 * 
	 * @param startLine the start line (first line) of the message
	 * @return the new HttpMessage object that is to hold the decoded data
	 * @throws ProtocolException
	 */
	protected abstract T newMessage(String startLine)
	        throws ProtocolException;
	
	/**
	 * Factory method to be implemented by derived classes that returns
	 * a decoder result as appropriate for the decoder.
	 * 
	 * @param headerCompleted indicates that the header has completely
	 * been received during the invocation of {@link #decode} that
	 * returned this result
	 * @param overflow {@code true} if the data didn't fit in the out buffer
	 * @param underflow {@code true} if more data is expected
	 * @param closeConnection {@code true} if the connection should be closed
	 */
	protected abstract DecoderResult newResult(boolean headerCompleted,
	        boolean overflow, boolean underflow, boolean closeConnection);
	
	/**
	 * Informs the derived class that the header has been received
	 * completely.
	 */
	protected abstract BodyMode headerReceived(T message)
	        throws ProtocolException;
	
	private DecoderResult createResult(boolean overflow,
	        boolean underflow, boolean closeConnection) {
		if (built != null && building != null) {
			building = null;
			return newResult(true, overflow, underflow, closeConnection);
		}
		return newResult(false, overflow, underflow, closeConnection);
	}
	
	/**
     * Decodes the next chunk of data.
     * 
     * @param in holds the data to be decoded
     * @param out gets the body data (if any) written to it
     * @return the result
     * @throws ProtocolException if the message violates the HTTP protocol
     */
	public DecoderResult decode(ByteBuffer in, ByteBuffer out)
			throws ProtocolException {
		try {
			return uncheckedDecode(in, out);
		} catch (ParseException | NumberFormatException e) {
			throw new ProtocolException(protocolVersion, 
					HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
		}
	}
		
	private DecoderResult uncheckedDecode(ByteBuffer in, ByteBuffer out)
			throws ProtocolException, ParseException {
		// May be invoked with null (end of body), but not with empty. Check
		// once.
		if (in != null && !in.hasRemaining()) {
			return createResult(false, true, false);
		}
		int stateLevel;
		do {
			stateLevel = states.size();
			switch (states.peek()) {
			// Waiting for CR (start of end of line)
			case RECEIVE_LINE: {
				byte ch = in.get();
				if (ch == '\r') {
					states.pop();
					states.push(State.AWAIT_LINE_END);
					continue;
				}
				lineBuilder.append(ch);
				// RFC 7230 3.2.5
				if (headerLength + lineBuilder.position() > maxHeaderLength) {
					throw new ProtocolException(protocolVersion,
					        HttpStatus.BAD_REQUEST.getStatusCode(),
					        "Maximum header size exceeded");
				}
				continue;
			}
			// Waiting for LF (confirmation of end of line)
			case AWAIT_LINE_END: {
				char ch = (char) in.get();
				if (ch == '\n') {
					try {
						// RFC 7230 3.2.4
						receivedLine = new String(lineBuilder.array(), 0,
						        lineBuilder.position(), "iso-8859-1");
					} catch (UnsupportedEncodingException e) {
						// iso-8859-1 is guaranteed to be supported
					}
					lineBuilder.clear();
					states.pop();
					continue;
				}
				throw new ProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "CR not followed by LF");
			}
			// Waiting for the initial request line
			case AWAIT_MESSAGE_START:
				if (receivedLine.isEmpty()) {
					// Ignore as recommended by RFC2616/RFC7230
					states.push(State.RECEIVE_LINE);
					continue;
				}
				building = newMessage(receivedLine);
				built = null;
				states.pop();
				headerLine = null;
				states.push(State.HEADER_LINE_RECEIVED);
				states.push(State.RECEIVE_LINE);
				continue;

			case HEADER_LINE_RECEIVED:
				if (headerLine != null) {
					// RFC 7230 3.2.4
					if (!receivedLine.isEmpty()
					        && (receivedLine.charAt(0) == ' '
					                || receivedLine.charAt(0) == '\t')) {
						headerLine += (" " + receivedLine.substring(1));
						states.push(State.RECEIVE_LINE);
						continue;
					}
					// Header line complete, evaluate
					newHeaderLine();
				}
				if (receivedLine.isEmpty()) {
					// Body starts
					built = building;
					BodyMode bm = headerReceived(building);
					adjustToBodyMode(bm);
					if (bm == BodyMode.NO_BODY) {
						adjustToEndOfMessage();
						return createResult(false, false, false);
					}
					if (out == null) {
						return createResult(true, false, false);
					}
					continue;
				}
				headerLine = receivedLine;
				states.push(State.RECEIVE_LINE);
				continue;

			case CONTENT_RECEIVED:
				// We "drop" to this state after READ_SPECIFIED
				states.pop();
				adjustToEndOfMessage();
				return createResult(false, false, false);
				
			case CHUNK_START_RECEIVED:
				// We "drop" to this state when a line has been read
				String sizeText = receivedLine.split(";")[0];
				long chunkSize = Long.parseLong(sizeText, 16);
				if (chunkSize == 0) {
					states.pop();
					states.push(State.CHUNK_TRAILER_LINE_RECEIVED);
					states.push(State.RECEIVE_LINE);
					continue;
				}
				leftToRead = chunkSize;
				// We expect the chunk data and the trailing CRLF (empty line)
				// (which must be skipped). In reverse order:
				states.push(State.CHUNK_END_RECEIVED);
				states.push(State.RECEIVE_LINE);
				states.push(State.COPY_SPECIFIED);
				continue;
				
			case CHUNK_END_RECEIVED:
				// We "drop" to this state when the CR/LF after chunk data
				// has been read. There's nothing to do except to wait for
				// next chunk
				if (receivedLine.length() != 0) {
					throw new ProtocolException(protocolVersion, 
							HttpStatus.BAD_REQUEST.getStatusCode(), 
							"No CRLF after chunk data.");
				}
				states.pop();
				states.push(State.CHUNK_START_RECEIVED);
				states.push(State.RECEIVE_LINE);
				continue;
				
			case CHUNK_TRAILER_LINE_RECEIVED:
				// We "drop" to this state when a line has been read
				if (!receivedLine.isEmpty()) {
					headerLine = receivedLine;
					newTrailerLine();
					states.push(State.RECEIVE_LINE);
					break;
				}
				// All chunked data received
				adjustToEndOfMessage();
				return createResult(false, false, false);
				
			case COPY_SPECIFIED:
				if (out == null) {
					return createResult(true, false, false);
				}
				int initiallyRemaining = in.remaining();
				if (out.remaining() <= leftToRead) {
					ByteBufferUtils.putAsMuchAsPossible(out, in);
				} else {
					ByteBufferUtils.putAsMuchAsPossible(out, in,
					        (int) leftToRead);
				}
				leftToRead -= (initiallyRemaining - in.remaining());
				if (leftToRead == 0) {
					states.pop();
					continue;
				}
				return createResult(!out.hasRemaining() && in.hasRemaining(),
				        !in.hasRemaining(), false);
				
			case COPY_UNTIL_CLOSED:
				if (in == null) {
					// Closed indication
					states.pop();
					states.push(State.CLOSED);
					return createResult(false, false, true);
				}
				if (out == null) {
					return createResult(true, false, false);
				}
				ByteBufferUtils.putAsMuchAsPossible(out, in);
				return createResult(!out.hasRemaining() && in.hasRemaining(),
				        true, false);

			case CLOSED:
				in.position(in.limit());
				return createResult(false, false, true);
			}
		} while (in.hasRemaining() || states.size() < stateLevel);
		return createResult(false, true, false);
	}

	private void newHeaderLine() throws ProtocolException, ParseException {
		headerLength += headerLine.length() + 2;
		// RFC 7230 3.2
		Matcher m = headerLinePatter.matcher(headerLine);
		if (!m.matches()) {
			throw new ProtocolException(protocolVersion, 
			        HttpStatus.BAD_REQUEST.getStatusCode(), "Invalid header");
		}
		String fieldName = m.group(1);
		// RFC 7230 3.2.4
		String fieldValue = m.group(2).trim();
		HttpField<?> field = HttpField.fromString(fieldName, fieldValue);
		switch (field.getName()) {
		case HttpField.CONTENT_LENGTH:
			// RFC 7230 3.3.3 (3.)
			if (building.fields()
			        .containsKey(HttpField.TRANSFER_ENCODING)) {
				field = null;
				break;
			}
			// RFC 7230 3.3.3 (4.)
			HttpContentLengthField existing = building.getField(
			        HttpContentLengthField.class, HttpField.CONTENT_LENGTH);
			if (existing != null && !existing.getValue()
			        .equals(((HttpContentLengthField) field).getValue())) {
				throw new ProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST);
			}
			break;
		case HttpField.TRANSFER_ENCODING:
			// RFC 7230 3.3.3 (3.)
			building.removeField(HttpField.CONTENT_LENGTH);
			break;
		}
		if (field == null) {
			return;
		}
		addHeaderField(field);
	}
    
	private void newTrailerLine() throws ProtocolException, ParseException {
		headerLength += headerLine.length() + 2;
		// RFC 7230 3.2
		Matcher m = headerLinePatter.matcher(headerLine);
		if (!m.matches()) {
			throw new ProtocolException(protocolVersion, 
			        HttpStatus.BAD_REQUEST.getStatusCode(), "Invalid header");
		}
		String fieldName = m.group(1);
		// RFC 7230 3.2.4
		String fieldValue = m.group(2).trim();
		HttpField<?> field = HttpField.fromString(fieldName, fieldValue);
		// RFC 7230 4.4
		HttpStringListField trailerField = building
		        .getField(HttpStringListField.class, HttpField.TRAILER);
		if (trailerField == null) {
			trailerField = new HttpStringListField(HttpField.TRAILER);
			building.setField(trailerField);
		}
		if (!trailerField.containsIgnoreCase(field.getName())) {
			trailerField.add(field.getName());
		}
		addHeaderField(field);
	}

	private void addHeaderField(HttpField<?> field)
	        throws ProtocolException, ParseException {
		// RFC 7230 3.2.2
		HttpField<?> existing = building.fields().get(field.getName());
		if (existing != null) {
			if (!(existing instanceof HttpListField<?>)
			        || !(field instanceof HttpListField<?>)
			        || !(existing.getClass().equals(field.getClass()))) {
				throw new ProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "Multiple occurences of field " + field.getName());
			}
			((HttpListField<?>) existing).combine((HttpListField<?>) field);
		} else {
			building.setField(field);
		}
	}
    
	private void adjustToBodyMode(BodyMode bm) {
		building.setMessageHasBody(bm != BodyMode.NO_BODY);
		states.pop();
		switch (bm) {
		case UNTIL_CLOSE:
			states.push(State.COPY_UNTIL_CLOSED);
			break;
		case CHUNKED:
			states.push(State.CHUNK_START_RECEIVED);
			states.push(State.RECEIVE_LINE);
			break;
		case LENGTH:
			HttpContentLengthField clf = building.getField(
			        HttpContentLengthField.class,
			        HttpField.CONTENT_LENGTH);
			leftToRead = clf.getValue();
			if (leftToRead > 0) {
				states.push(State.CONTENT_RECEIVED);
				states.push(State.COPY_SPECIFIED);
				break;
			}
			// Length == 0 means no body, fall through
		case NO_BODY:
			break;
		}
	}

	private void adjustToEndOfMessage() {
		// RFC 7230 6.3
		HttpStringListField connection = built
		        .getField(HttpStringListField.class, HttpField.CONNECTION);
		if (connection != null && connection.containsIgnoreCase("close")) {
			states.push(State.CLOSED);
			return;
		}
		if (built.getProtocol().compareTo(HttpProtocol.HTTP_1_1) >= 0) {
			states.push(State.AWAIT_MESSAGE_START);
			states.push(State.RECEIVE_LINE);
			return;
		}
		states.push(State.CLOSED);
	}
}
