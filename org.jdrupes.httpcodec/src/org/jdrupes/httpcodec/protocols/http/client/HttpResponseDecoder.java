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
package org.jdrupes.httpcodec.protocols.http.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.Engine;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;

/**
 * A decoder for HTTP responses that accepts data from a sequence of
 * {@link ByteBuffer}s.
 * 
 * @author Michael N. Lipp
 */
public class HttpResponseDecoder 
	extends HttpDecoder<HttpResponse, ResponseDecoder.Result>
	implements ResponseDecoder<HttpResponse, HttpRequest> {

	// RFC 7230 3.1.2
	private final static Pattern responseLinePatter = Pattern
	        .compile("^(" + HTTP_VERSION + ")" + SP + "([1-9][0-9][0-9])"
	                + SP + "(.*)$");

	private String requestMethod = "";
	
	/**
	 * Creates a new decoder that belongs to the given HTTP engine.
	 * 
	 * @param engine the engine
	 */
	public HttpResponseDecoder(Engine engine) {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.HttpDecoder#createResult(org.jdrupes.httpcodec.
	 * HttpMessage, boolean, boolean, boolean)
	 */
	@Override
	protected ResponseDecoder.Result newResult(boolean headerCompleted,
	        boolean overflow, boolean underflow) {
		return new ResponseDecoder.Result
				(headerCompleted, overflow, underflow, isClosed(),
				 null, null, null);
	}

	/**
	 * Starts decoding a new response to a given request.
	 * Specifying the request is necessary because the existence of a body
	 * cannot be derived by looking at the header only. It depends on the kind
	 * of request made. Must be called before the response is decoded.
	 * 
	 * @param request
	 *            the request
	 */
	public void decodeResponseTo(HttpRequest request) {
		this.requestMethod = request.getMethod();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Decoder#decode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public ResponseDecoder.Result decode
		(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws HttpProtocolException {
		return super.decode(in, out, endOfInput);
	}

	/**
	 * Checks whether the first line of a message is a valid response.
	 * If so, create a new response message object with basic information, else
	 * throw an exception.
	 * <P>
	 * Called by the base class when a first line is received.
	 * 
	 * @param startLine the first line
	 * @throws HttpProtocolException if the line is not a correct request line
	 */
	@Override
	protected HttpResponse newMessage(String startLine)
	        throws HttpProtocolException {
		Matcher responseMatcher = responseLinePatter.matcher(startLine);
		if (!responseMatcher.matches()) {
			throw new HttpProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.getStatusCode(),
			        "Illegal request line");
		}
		String httpVersion = responseMatcher.group(1);
		int statusCode = Integer.parseInt(responseMatcher.group(2));
		String reasonPhrase = responseMatcher.group(3);
		boolean found = false;
		for (HttpProtocol v : HttpProtocol.values()) {
			if (v.toString().equals(httpVersion)) {
				protocolVersion = v;
				found = true;
			}
		}
		if (!found) {
			throw new HttpProtocolException(HttpProtocol.HTTP_1_1,
			        HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		}
		return new HttpResponse(protocolVersion, statusCode, reasonPhrase,
		        false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.
	 * HttpMessage)
	 */
	@Override
	protected BodyMode headerReceived(HttpResponse message)
	        throws HttpProtocolException {
		// RFC 7230 3.3.3 (1. & 2.)
		int statusCode = message.getStatusCode();
		if (requestMethod.equalsIgnoreCase("HEAD")
		        || (statusCode % 100) == 1
		        || statusCode == 204
		        || statusCode == 304
		        || (requestMethod.equalsIgnoreCase("CONNECT")
		                && (statusCode % 100 == 2))) {
			return BodyMode.NO_BODY;
		}
		HttpStringListField transEncs = message.getField(
		        HttpStringListField.class, HttpField.TRANSFER_ENCODING)
				.orElse(null);
		// RFC 7230 3.3.3 (3.)
		if (transEncs != null) {
			if (transEncs.get(transEncs.size() - 1)
			        .equalsIgnoreCase(TransferCoding.CHUNKED.toString())) {
				return BodyMode.CHUNKED;
			} else {
				return BodyMode.UNTIL_CLOSE;
			}
		}
		// RFC 7230 3.3.3 (5.)
		if (message.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			return BodyMode.LENGTH;
		}
		// RFC 7230 3.3.3 (7.)
		return BodyMode.UNTIL_CLOSE;
	}

}
