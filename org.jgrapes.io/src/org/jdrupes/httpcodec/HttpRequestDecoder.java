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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpRequestDecoder extends HttpCodec {

	final private static String TOKEN 
		= "[!#\\$%&'\\*\\+-\\.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "\\^`abcdefghijklmnopqrstuvwxyz\\|~]+";
	final private static String SP = "[ \\t]+";
	final private static String HTTP_VERSION = "HTTP/\\d+\\.\\d";
	
	final static Pattern requestLinePatter = Pattern.compile("^(" + TOKEN 
			+ ")" + SP + "([^ \\t])+" + SP + "(" + HTTP_VERSION + ")$");
	
    private enum State {
        AWAIT_CR, AWAIT_LF, AWAIT_REQUEST, GETTING_HEADERS, RECEIVING_BODY
    }
    
    private Stack<State> states = new Stack<>();
    private StringBuilder lineBuilder = new StringBuilder(8192);
    private String receivedLine;
    private String headerLine = null;
    private HttpProtocol protocolVersion = HttpProtocol.HTTP_1_0;
    private HttpRequest building;
    private HttpRequest decodedRequest = null;
    
    public HttpRequestDecoder() {
    	states.push(State.AWAIT_REQUEST);
    	states.push(State.AWAIT_CR);
	}

    public void decode(ByteBuffer buffer) throws ProtocolException {
    	int stateLevel = states.size();
		while (buffer.hasRemaining() || states.size() < stateLevel) {
			stateLevel = states.size();
			switch (states.peek()) {
			// Waiting for CR (start of end of line)
			case AWAIT_CR: {
				char ch = (char) buffer.get();
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
					receivedLine = lineBuilder.toString();
					lineBuilder.delete(0, lineBuilder.length());
					states.pop();
					continue;
				}
				throw new ProtocolException (protocolVersion, 
					HttpStatus.BAD_REQUEST.getCode(), "CR not followed by LF");
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
				headerLine = "";
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
					evaluateHeader();
				}
				if (receivedLine.isEmpty()) {
					// last header, body starts
					decodedRequest = building;
					states.pop();
					states.push(State.RECEIVING_BODY);
					continue;
				}
				headerLine = receivedLine;
				states.push(State.AWAIT_CR);
				continue;
			}
		}
	}

	private void startNewRequest() throws ProtocolException {
		Matcher requestMatcher = requestLinePatter.matcher(receivedLine);
		if (!requestMatcher.matches()) {
			throw new ProtocolException (protocolVersion, 
					HttpStatus.BAD_REQUEST.getCode(), "Illegal request line");
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
				(protocolVersion, HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		}
		URI uri = null;
		if (uriGroup.equals("*")) {
			uri = HttpRequest.ASTERISK_REQUEST;
		} else {
			try {
				uri = new URI(uriGroup);
			} catch (URISyntaxException e) {
				throw new ProtocolException (protocolVersion, 
						HttpStatus.BAD_REQUEST.getCode(), e.getMessage());
			}
		}
		building = new HttpRequest(method, uri, protocolVersion);
	}

	private void evaluateHeader() {
	}

	/**
	 * If a previous invocation of {@link #decode(ByteBuffer)} resulted
	 * in a fully decoded request, return it and reset the internal storage
	 * for the internal request.
	 * 
	 * @return the request, if available
	 */
	public HttpRequest decodedRequest() {
		if (decodedRequest != null) {
			HttpRequest result = decodedRequest;
			decodedRequest = null;
			return result;
		}
		return null;
	}
}
