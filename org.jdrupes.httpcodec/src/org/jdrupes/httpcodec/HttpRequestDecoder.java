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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.util.DynamicByteArray;
import org.jdrupes.httpcodec.util.HttpUtils;

/**
 * A decoder for HTTP requests that accepts data from a sequence of
 * {@link ByteBuffer}s.
 * 
 * @author Michael N. Lipp
 */
public class HttpRequestDecoder {

	final private static String TOKEN 
		= "[" + Pattern.quote(HttpUtils.TCHARS) + "]+";
	final private static String SP = "[ \\t]+";
	final private static String HTTP_VERSION = "HTTP/\\d+\\.\\d";
	
	final static Pattern requestLinePatter = Pattern.compile("^(" + TOKEN 
			+ ")" + SP + "([^ \\t]+)" + SP + "(" + HTTP_VERSION + ")$");
	final static Pattern headerLinePatter 
			= Pattern.compile("^(" + TOKEN + "):(.*)$");
	
    private enum State {
        AWAIT_CR, AWAIT_LF, AWAIT_REQUEST, GETTING_HEADERS, RECEIVING_BODY
    }
    
    private long maxHeaderLength = 4194304;

    private Stack<State> states = new Stack<>();
    private DynamicByteArray lineBuilder = new DynamicByteArray(8192);
    private String receivedLine;
    private String headerLine = null;
    private HttpProtocol protocolVersion = HttpProtocol.HTTP_1_0;
    private HttpRequest building;
    private long headerLength = 0;
    private boolean hasBody = false;

    /**
     * Creates a new decoder.
     */
    public HttpRequestDecoder() {
    	states.push(State.AWAIT_REQUEST);
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
     * Decodes the next chunk of data.
     * 
     * @param buffer holds the data to be decoded
     * @return the result
     */
	public DecoderResult decode(ByteBuffer buffer) {
		try {
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
				case AWAIT_REQUEST:
					if (receivedLine.isEmpty()) {
						// Ignore as recommended by RFC2616
						states.push(State.AWAIT_CR);
						continue;
					}
					startNewRequest();
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
						DecoderResult result = new DecoderResult(building,
						        hasBody, false, null, false);						
						states.pop();
						if (hasBody) {
							states.push(State.RECEIVING_BODY);
						} else {
					    	states.push(State.AWAIT_REQUEST);
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
		} catch (ProtocolException e) {
			HttpResponse response = new HttpResponse(e.getHttpVersion(), 
					HttpStatus.BAD_REQUEST, false);
			response.setStatusCode(e.getStatusCode());
			response.setReasonPhrase(e.getReasonPhrase());
			response.setHeader(
			        new HttpStringListField(HttpField.CONNECTION, "close"));
			return new DecoderResult(null, false, false, response, true);
		}
		return new DecoderResult(null, false, false, null, false);
	}

	private void startNewRequest() throws ProtocolException {
		Matcher requestMatcher = requestLinePatter.matcher(receivedLine);
		if (!requestMatcher.matches()) {
			throw new ProtocolException (protocolVersion, 
					HttpStatus.BAD_REQUEST.getStatusCode(), "Illegal request line");
		}
		String method = requestMatcher.group(1);
		String uriGroup = requestMatcher.group(2);
		String httpVersion = requestMatcher.group(3);
		boolean found = false;
		for (HttpProtocol v : HttpProtocol.values()) {
			if (v.toString().equals(httpVersion)) {
				protocolVersion = v;
				found = true;
			}
		}
		if (!found) {
			throw new ProtocolException
				(HttpProtocol.HTTP_1_1, HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		}
		URI uri = null;
		if (uriGroup.equals("*")) {
			uri = HttpRequest.ASTERISK_REQUEST;
		} else {
			try {
				uri = new URI(uriGroup);
			} catch (URISyntaxException e) {
				throw new ProtocolException (protocolVersion, 
						HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
			}
		}
		building = new HttpRequest(method, uri, protocolVersion);
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
			case HttpField.HOST:
				String[] hostPort = fieldValue.split(":");
				try {
					building.setHostAndPort(hostPort[0], 
							Integer.parseInt(hostPort[1]));
				} catch (NumberFormatException e) {
					throw new ParseException(fieldValue, 0);
				}
				break;
			case HttpField.TRANSFER_ENCODING:
				hasBody = true;
				break;
			case HttpField.CONTENT_LENGTH:
				hasBody = true;
				break;
			case HttpField.CONNECTION:
				if (((HttpStringListField)field).containsIgnoreCase("close")) {
					building.getResponse().setHeader(new HttpStringListField(
					        HttpField.CONNECTION, "close"));
				}
				break;
			}
			building.setHeader(field);
		} catch (ParseException e) {
			throw new ProtocolException(protocolVersion, 
					HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
		}
	}
}
