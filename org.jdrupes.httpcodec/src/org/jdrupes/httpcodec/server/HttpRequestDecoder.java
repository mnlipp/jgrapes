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
package org.jdrupes.httpcodec.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpStringField;
import org.jdrupes.httpcodec.fields.HttpStringListField;
import org.jdrupes.httpcodec.internal.Decoder;
import org.jdrupes.httpcodec.internal.DecoderResult;
import org.jdrupes.httpcodec.internal.Engine;

/**
 * A decoder for HTTP requests that accepts data from a sequence of
 * {@link ByteBuffer}s.
 * 
 * @author Michael N. Lipp
 */
public class HttpRequestDecoder extends Decoder<HttpRequest> {

	// RFC 7230 3.1.1
	private final static Pattern requestLinePatter = Pattern
	        .compile("^(" + TOKEN + ")" + SP + "([^ \\t]+)" + SP + "("
	                + HTTP_VERSION + ")$");

	/**
	 * Creates a new encoder that belongs to the given HTTP engine.
	 * 
	 * @param engine the engine
	 */
	public HttpRequestDecoder(Engine<HttpRequest, HttpResponse> engine) {
		super(engine);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#createResult(org.jdrupes.httpcodec.HttpMessage, boolean, boolean, boolean)
	 */
	@Override
	protected DecoderResult newResult(boolean headerCompleted,
	        boolean overflow, boolean underflow) {
		return new Result(headerCompleted, null, overflow, underflow);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#decode(java.nio.ByteBuffer)
	 */
	@Override
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput) {
		try {
			return (Result) super.decode(in, out, endOfInput);
		} catch (ProtocolException e) {
			HttpResponse response = new HttpResponse(e.getHttpVersion(), 
					e.getStatusCode(), e.getReasonPhrase(), false);
			response.setField(
			        new HttpStringListField(HttpField.CONNECTION, "close"));
			return new Result(false, response, false, false);
		}
	}

	/**
	 * Checks whether the first line of a message is a valid request.
	 * If so, create a new request message object with basic information, else
	 * throw an exception.
	 * <P>
	 * Called by the base class when a first line is received.
	 * 
	 * @param startLine the first line
	 * @throws ProtocolException if the line is not a correct request line
	 */
	@Override
	protected HttpRequest newMessage(String startLine)
	        throws ProtocolException {
		Matcher requestMatcher = requestLinePatter.matcher(startLine);
		if (!requestMatcher.matches()) {
			// RFC 7230 3.1.1
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

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.HttpMessage)
	 */
	@Override
	protected BodyMode headerReceived(HttpRequest message) 
			throws ProtocolException {
		// Handle field of special interest
		HttpStringField host = message.getField(HttpStringField.class,
		        HttpField.HOST);
		if (host != null) {
			String[] hostPort = ((HttpStringField)host).getValue().split(":");
			try {
				message.setHostAndPort(hostPort[0], 
						Integer.parseInt(hostPort[1]));
			} catch (NumberFormatException e) {
				throw new ProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "Invalid Host port.");
			}
		} else {
			// RFC 7230 5.4.
			if (message.getProtocol().compareTo(HttpProtocol.HTTP_1_1) >= 0) {
				throw new ProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "HTTP 1.1 request must have a Host field.");
			}
		}
		HttpStringListField connection = message
		        .getField(HttpStringListField.class, HttpField.CONNECTION);
		if (connection != null && connection.containsIgnoreCase("close")) {
			// RFC 7230 6.6.
			message.getResponse().setField(new HttpStringListField(
			        HttpField.CONNECTION, "close"));
		}

		// Find out about body
		HttpStringListField transEncs = message.getField(
		        HttpStringListField.class, HttpField.TRANSFER_ENCODING);
		if (transEncs != null) {
			// RFC 7230 3.3.1
			HttpStringListField tec = transEncs.clone();
			tec.removeIgnoreCase(TransferCoding.CHUNKED.toString());
			if (tec.size() > 0) {
				throw new ProtocolException(protocolVersion,
				        HttpStatus.NOT_IMPLEMENTED);
			}
			// RFC 7230 3.3.3 (3.)
			if (transEncs != null) {
				if (transEncs.get(transEncs.size() - 1)
				        .equalsIgnoreCase(TransferCoding.CHUNKED.toString())) {
					return BodyMode.CHUNKED;
				} else {
					throw new ProtocolException(protocolVersion,
					        HttpStatus.BAD_REQUEST);
				}
			}
		}
		// RFC 7230 3.3.3 (5.)
		if (message.fields().containsKey(HttpField.CONTENT_LENGTH)) {
			return BodyMode.LENGTH;
		}
		// RFC 7230 3.3.3 (6.)
		return BodyMode.NO_BODY;
	}

	public class Result extends DecoderResult {

		private HttpResponse response;

		/**
		 * Creates a new result.
		 * @param response a response to send due to an error
		 * @param overflow {@code true} if the data didn't fit in the out buffer
		 * @param underflow {@code true} if more data is expected
		 * @param message the decoded message
		 */
		public Result(boolean headerCompleted, HttpResponse response,
		        boolean overflow, boolean underflow) {
			super(headerCompleted, overflow, underflow);
			this.response = response;
		}

		/**
		 * Returns {@code true} if the result includes a response
		 * (see @link #getResponse()}.
		 * 
		 * @return the result
		 */
		public boolean hasResponse() {
			return response != null;
		}
		
		/**
		 * Returns the response if a response exists. A response in
		 * the decoder result indicates that some problem occurred that
		 * must be signaled back to the client.
		 * 
		 * @return the response
		 */
		public HttpResponse getResponse() {
			return response;
		}

	}
}
