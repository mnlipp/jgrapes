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
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpStringField;
import org.jdrupes.httpcodec.fields.HttpStringListField;

/**
 * A decoder for HTTP requests that accepts data from a sequence of
 * {@link ByteBuffer}s.
 * 
 * @author Michael N. Lipp
 */
public class HttpRequestDecoder extends HttpDecoder<HttpRequest> {

	final static Pattern requestLinePatter = Pattern.compile("^(" + TOKEN 
			+ ")" + SP + "([^ \\t]+)" + SP + "(" + HTTP_VERSION + ")$");

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#createResult(org.jdrupes.httpcodec.HttpMessage, boolean, boolean, boolean)
	 */
	@Override
	protected DecoderResult<HttpRequest> createResult(HttpRequest message,
	        boolean payloadBytes, boolean payloadChars,
	        boolean closeConnection) {
		return new RequestResult(message, payloadBytes, payloadChars, 
				null, closeConnection);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#decode(java.nio.ByteBuffer)
	 */
	@Override
	public RequestResult decode(ByteBuffer buffer) {
		try {
			return (RequestResult) super.decode(buffer);
		} catch (ProtocolException e) {
			HttpResponse response = new HttpResponse(e.getHttpVersion(), 
					HttpStatus.BAD_REQUEST, false);
			response.setStatusCode(e.getStatusCode());
			response.setReasonPhrase(e.getReasonPhrase());
			response.setHeader(
			        new HttpStringListField(HttpField.CONNECTION, "close"));
			return new RequestResult(null, false, false, response, true);
		}
	}

	protected HttpRequest newMessage(String startLine)
	        throws ProtocolException {
		Matcher requestMatcher = requestLinePatter.matcher(startLine);
		if (!requestMatcher.matches()) {
			throw new ProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.getStatusCode(),
			        "Illegal request line");
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
			throw new ProtocolException(HttpProtocol.HTTP_1_1,
			        HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		}
		URI uri = null;
		if (uriGroup.equals("*")) {
			uri = HttpRequest.ASTERISK_REQUEST;
		} else {
			try {
				uri = new URI(uriGroup);
			} catch (URISyntaxException e) {
				throw new ProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
			}
		}
		return new HttpRequest(method, uri, protocolVersion, false);
	}

	protected void newField(HttpField<?> field)
			throws ProtocolException, ParseException {
		switch (field.getName()) {
		case HttpField.HOST:
			String[] hostPort = ((HttpStringField)field).getValue().split(":");
			try {
				getBuilding().setHostAndPort(hostPort[0], 
						Integer.parseInt(hostPort[1]));
			} catch (NumberFormatException e) {
				throw new ParseException(field.getValue().toString(), 0);
			}
			break;
		case HttpField.CONNECTION:
			if (((HttpStringListField)field).containsIgnoreCase("close")) {
				getBuilding().getResponse().setHeader(new HttpStringListField(
				        HttpField.CONNECTION, "close"));
			}
			break;
		}
	}
	
}
