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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpListField;
import org.jdrupes.httpcodec.util.DynamicByteArray;
import org.jdrupes.httpcodec.util.HttpUtils;

/**
 * @author Michael N. Lipp
 *
 */
abstract class HttpDecoder<T extends HttpMessage> {

	final protected static String TOKEN = "[" + Pattern.quote(HttpUtils.TCHARS)
	        + "]+";
	final protected static String SP = "[ \\t]+";
	final protected static String HTTP_VERSION = "HTTP/\\d+\\.\\d";

	final protected static Pattern headerLinePatter = Pattern
	        .compile("^(" + TOKEN + "):(.*)$");
	
    private enum State {
        AWAIT_CR, AWAIT_LF, AWAIT_MESSAGE, GETTING_HEADERS, RECEIVING_BODY
    }
    
    private long maxHeaderLength = 4194304;

    private Stack<State> states = new Stack<>();
    private DynamicByteArray lineBuilder = new DynamicByteArray(8192);
    private String receivedLine;
    private String headerLine = null;
    protected HttpProtocol protocolVersion = HttpProtocol.HTTP_1_0;
    private long headerLength = 0;
    private boolean hasBody = false;
    private T building;
	

    /**
     * Creates a new decoder.
     */
    public HttpDecoder() {
    	states.push(State.AWAIT_MESSAGE);
    	states.push(State.AWAIT_CR);
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
	 * Informs the derived class about the start of a new message.
	 * 
	 * @param startLine the start line (first line) of the message
	 * @return the new HttpMessage object that is to hold the decoded data
	 * @throws ProtocolException
	 */
	protected abstract T newMessage(String startLine)
	        throws ProtocolException;
	
	/**
	 * Informs a derived class about a new header field. The new header 
	 * will be added to the headers after this method returns.
	 * 
	 * @param field
	 * @throws ProtocolException
	 */
	protected void newField(HttpField<?> field) 
			throws ProtocolException, ParseException {
	}

	/**
	 * Returns the HttpMessage object being build by the decoder.
	 * 
	 * @return
	 */
	protected T getBuilding() {
		return building;
	}

	/**
	 * Factory method to be implemented by derived classes that returs
	 * a decoder result as appropriate for the decoder.
	 * 
	 * @param message the decoded message
	 * @param response a response to send because an error occurred
	 * that must be signaled back to the client
	 * @param payloadBytes {@code true} if the request has a body with octets
	 * @param payloadChars {@code true} if the request has a body with text
	 */
	protected abstract DecoderResult<T> createResult(T message,
	        boolean payloadBytes, boolean payloadChars,
	        boolean closeConnection);
	
	/**
     * Decodes the next chunk of data.
     * 
     * @param buffer holds the data to be decoded
     * @return the result
     * @throws ProtocolException if the message violates the HTTP protocol
     */
	public DecoderResult<T> decode(ByteBuffer buffer) throws ProtocolException {
		int stateLevel = states.size();
		while (buffer.hasRemaining() || states.size() < stateLevel) {
			stateLevel = states.size();
			switch (states.peek()) {
			// Waiting for CR (start of end of line)
			case AWAIT_CR: {
				byte ch = buffer.get();
				if (ch == '\r') {
					states.pop();
					states.push(State.AWAIT_LF);
					continue;
				}
				lineBuilder.append(ch);
				continue;
			}
			// Waiting for LF (confirmation of end of line)
			case AWAIT_LF: {
				char ch = (char) buffer.get();
				if (ch == '\n') {
					try {
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
			case AWAIT_MESSAGE:
				if (receivedLine.isEmpty()) {
					// Ignore as recommended by RFC2616/RFC7230
					states.push(State.AWAIT_CR);
					continue;
				}
				building = newMessage(receivedLine);
				states.pop();
				headerLine = null;
				states.push(State.GETTING_HEADERS);
				states.push(State.AWAIT_CR);
				continue;

			case GETTING_HEADERS:
				if (headerLine != null) {
					if (!receivedLine.isEmpty()
					        && (receivedLine.charAt(0) == ' '
					                || receivedLine.charAt(0) == '\t')) {
						headerLine += (" " + receivedLine);
						states.push(State.AWAIT_CR);
						continue;
					}
					// Header line complete, evaluate
					evaluateHeaderLine();
				}
				if (receivedLine.isEmpty()) {
					// last header, body starts
					DecoderResult<T> result = createResult(building,
					        hasBody, false, false);
					states.pop();
					if (hasBody) {
						states.push(State.RECEIVING_BODY);
					} else {
						states.push(State.AWAIT_MESSAGE);
						states.push(State.AWAIT_CR);
					}
					return result;
				}
				headerLine = receivedLine;
				states.push(State.AWAIT_CR);
				continue;
			}
		}
		if (lineBuilder.position() > maxHeaderLength) {
			throw new ProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.getStatusCode(),
			        "Maximum header size exceeded");
		}
		return createResult(null, false, false, false);
	}

	private void evaluateHeaderLine() throws ProtocolException {
		headerLength += headerLine.length() + 2;
		if (headerLength > maxHeaderLength) {
			throw new ProtocolException(protocolVersion, 
					HttpStatus.BAD_REQUEST.getStatusCode(), 
					"Maximum header size exceeded");
		}
		Matcher m = headerLinePatter.matcher(headerLine);
		if (!m.matches()) {
			throw new ProtocolException(protocolVersion, 
					HttpStatus.BAD_REQUEST.getStatusCode(), "Invalid header");
		}
		String fieldName = m.group(1);
		String fieldValue = m.group(2).trim();
		try {
			HttpField<?> field = HttpField.fromString(fieldName, fieldValue);
			// Use normalized field name for switch
			switch (field.getName()) {
			case HttpField.TRANSFER_ENCODING:
				hasBody = true;
				break;
			case HttpField.CONTENT_LENGTH:
				hasBody = true;
				break;
			}
			newField(field);
			HttpField<?> existing = building.headers().get(field.getName());
			// RFC 7230 3.2.2
			if (existing != null) {
				if (!(existing instanceof HttpListField<?>)
						|| !(field instanceof HttpListField<?>)
						|| !(existing.getClass().equals(field.getClass()))) {
					throw new ProtocolException(protocolVersion, 
							HttpStatus.BAD_REQUEST.getStatusCode(),
							"Multiple occurences of field " + field.getName());
				}
				((HttpListField<?>)existing).combine((HttpListField<?>)field);
			} else {
				building.setHeader(field);
			}
		} catch (ParseException e) {
			throw new ProtocolException(protocolVersion, 
					HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
		}
	}
    
}
