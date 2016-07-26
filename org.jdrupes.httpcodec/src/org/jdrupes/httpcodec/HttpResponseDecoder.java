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

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	// RFC 7230 3.1.2
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
	        boolean overflow, boolean underflow) {
		return new Result(headerCompleted, overflow, underflow, isClosed());
	}

	/**
	 * Starts decoding a new response to a request with the given method.
	 * Specifying the request is necessary because the existence of a body
	 * cannot be derived by looking at the header only. It depends on the kind
	 * of request made.
	 * 
	 * @param requestMethod
	 *            the request method
	 * @return the result, which will always indicate underflow because more
	 *         input is required
	 */
	public DecoderResult decodeResponseTo(String requestMethod) {
		this.requestMethod = requestMethod;
		return new Result(false, false, true, false);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.internal.Decoder#decode(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public Result decode(ByteBuffer in, ByteBuffer out)
	        throws ProtocolException {
		return (Result)super.decode(in, out);
	}

	/**
	 * Checks whether the first line of a message is a valid response.
	 * If so, create a new response message object with basic information, else
	 * throw an exception.
	 * <P>
	 * Called by the base class when a first line is received.
	 * 
	 * @param startLine the first line
	 * @throws ProtocolException if the line is not a correct request line
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.
	 * HttpMessage)
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
		HttpStringListField transEncs = message.getField(
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
		if (message.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			return BodyMode.LENGTH;
		}
		// RFC 7230 3.3.3 (7.)
		return BodyMode.UNTIL_CLOSE;
	}

	public class Result extends DecoderResult {

		boolean closeConnection;
		
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
		public Result(boolean headerCompleted, boolean overflow,
		        boolean underflow, boolean closeConnection) {
			super(headerCompleted, overflow, underflow);
			this.closeConnection = closeConnection;
		}

		/**
		 * Indicates that the connection to the sender of the response must be
		 * closed.
		 * 
		 * @return the value
		 */
		public boolean getCloseConnection() {
			return closeConnection;
		}
	}
}
