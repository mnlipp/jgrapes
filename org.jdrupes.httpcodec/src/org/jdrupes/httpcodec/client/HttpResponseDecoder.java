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
package org.jdrupes.httpcodec.client;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.HttpCodec.HttpProtocol;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.HttpCodec.TransferCoding;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.internal.Decoder;
import org.jdrupes.httpcodec.internal.DecoderResult;

/**
 * A decoder for HTTP responses that accepts data from a sequence of
 * {@link ByteBuffer}s.
 * 
 * @author Michael N. Lipp
 */
public class HttpResponseDecoder extends Decoder<HttpResponse> {

	private final static Pattern responseLinePatter = Pattern
	        .compile("^(" + HTTP_VERSION + ")" + SP + "([1-9][0-9][0-9])"
	        		+ SP + "(.*)$");

	private String requestMethod = "";
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.HttpDecoder#createResult(org.jdrupes.httpcodec.
	 * HttpMessage, boolean, boolean, boolean)
	 */
	@Override
	protected DecoderResult newResult(boolean headerCompleted,
	        boolean overflow, boolean underflow,
	        boolean closeConnection) {
		return new Result(headerCompleted, overflow, underflow,
		        closeConnection);
	}

	/**
     * Starts decoding a new response for a request with the given method.
     * 
     * @param requestMethod the request method
     * @return the result
     */
	public DecoderResult decode(String requestMethod) {
		this.requestMethod = requestMethod;
		return new Result(false, false, false, false);
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#newMessage(java.lang.String)
	 */
	@Override
	protected HttpResponse newMessage(String startLine)
	        throws ProtocolException {
		Matcher requestMatcher = responseLinePatter.matcher(startLine);
		if (!requestMatcher.matches()) {
			throw new ProtocolException(protocolVersion,
			        HttpStatus.BAD_REQUEST.getStatusCode(),
			        "Illegal request line");
		}
		String httpVersion = requestMatcher.group(1);
		int statusCode = Integer.parseInt(requestMatcher.group(2));
		String reasonPhrase = requestMatcher.group(3);
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
		return new HttpResponse(protocolVersion, statusCode, reasonPhrase,
		        false);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.HttpMessage)
	 */
	@Override
	protected BodyMode headerReceived(HttpResponse message) 
			throws ProtocolException {
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
		HttpStringListField transEncs = message.getHeader(
		        HttpStringListField.class, HttpField.TRANSFER_ENCODING);
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
		if (message.headers().containsKey(HttpField.CONTENT_LENGTH)) {
			return BodyMode.LENGTH;
		}
		// RFC 7230 3.3.3 (7.)
		return BodyMode.UNTIL_CLOSE;
	}

	public class Result extends DecoderResult {

		/**
		 * Returns a new result.
		 * 
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 */
		public Result(boolean headerCompleted, boolean payloadBytes,
		        boolean payloadChars, boolean closeConnection) {
			super(headerCompleted, payloadBytes, payloadChars, closeConnection);
		}

	}
}
