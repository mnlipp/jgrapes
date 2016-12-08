/*******************************************************************************
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.jdrupes.httpcodec.protocols.http.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.protocols.http.HttpDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpStringListField;

/**
 * A decoder for HTTP requests that accepts data from a sequence of
 * {@link ByteBuffer}s.
 * 
 * @author Michael N. Lipp
 */
public class HttpRequestDecoder 
	extends HttpDecoder<HttpRequest, HttpResponse> {

	// RFC 7230 3.1.1
	private final static Pattern requestLinePatter = Pattern
	        .compile("^(" + TOKEN + ")" + SP + "([^ \\t]+)" + SP + "("
	                + HTTP_VERSION + ")$");

	/**
	 * Creates a new encoder that belongs to the given HTTP engine.
	 */
	public HttpRequestDecoder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#decode(java.nio.ByteBuffer)
	 */
	@Override
	public Result decode (ByteBuffer in, Buffer out, boolean endOfInput) {
		try {
			return (Result)super.decode(in, out, endOfInput);
		} catch (HttpProtocolException e) {
			HttpResponse response = new HttpResponse(e.getHttpVersion(), 
					e.getStatusCode(), e.getReasonPhrase(), false);
			response.setField(
			        new HttpStringListField(HttpField.CONNECTION, "close"));
			return newResult(false, false, false, response, true);
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
	 * @throws HttpProtocolException if the line is not a correct request line
	 */
	@Override
	protected HttpRequest newMessage(String startLine)
	        throws HttpProtocolException {
		Matcher requestMatcher = requestLinePatter.matcher(startLine);
		if (!requestMatcher.matches()) {
			// RFC 7230 3.1.1
			throw new HttpProtocolException(protocolVersion,
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
			throw new HttpProtocolException(HttpProtocol.HTTP_1_1,
			        HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
		}
		URI uri = null;
		if (uriGroup.equals("*")) {
			uri = HttpRequest.ASTERISK_REQUEST;
		} else {
			try {
				uri = new URI(uriGroup);
			} catch (URISyntaxException e) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(), e.getMessage());
			}
		}
		HttpRequest request = new HttpRequest
				(method, uri, protocolVersion, false);
		HttpResponse response = (new HttpResponse(protocolVersion,
				HttpStatus.NOT_IMPLEMENTED, false)).setRequest(request); 
		return request.setResponse(response);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.HttpDecoder#headerReceived(org.jdrupes.httpcodec.HttpMessage)
	 */
	@Override
	protected BodyMode headerReceived(HttpRequest message) 
			throws HttpProtocolException {
		// Handle field of special interest
		Optional<HttpStringField> host = message.getField
				(HttpStringField.class, HttpField.HOST);
		if (host.isPresent()) {
			String[] hostPort = host.get().getValue().split(":");
			try {
				message.setHostAndPort(hostPort[0], 
						Integer.parseInt(hostPort[1]));
			} catch (NumberFormatException e) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "Invalid Host port.");
			}
		} else {
			// RFC 7230 5.4.
			if (message.getProtocol().compareTo(HttpProtocol.HTTP_1_1) >= 0) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.BAD_REQUEST.getStatusCode(),
				        "HTTP 1.1 request must have a Host field.");
			}
		}
		if (message.getField(HttpStringListField.class, HttpField.CONNECTION)
				.map(f -> f.containsIgnoreCase("close")).orElse(false)) {
			// RFC 7230 6.6.
			message.getResponse().get().setField(new HttpStringListField(
			        HttpField.CONNECTION, "close"));
		}

		// Find out about body
		HttpStringListField transEncs = message.getField(
		        HttpStringListField.class, HttpField.TRANSFER_ENCODING)
				.orElse(null);
		if (transEncs != null) {
			// RFC 7230 3.3.1
			HttpStringListField tec = transEncs.clone();
			tec.removeIgnoreCase(TransferCoding.CHUNKED.toString());
			if (tec.size() > 0) {
				throw new HttpProtocolException(protocolVersion,
				        HttpStatus.NOT_IMPLEMENTED);
			}
			// RFC 7230 3.3.3 (3.)
			if (transEncs != null) {
				if (transEncs.get(transEncs.size() - 1)
				        .equalsIgnoreCase(TransferCoding.CHUNKED.toString())) {
					return BodyMode.CHUNKED;
				} else {
					throw new HttpProtocolException(protocolVersion,
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

	/**
	 * Overrides the base interface's factory method in order to make
	 * it return the extended return type. As the {@link HttpRequestDecoder}
	 * does not know about a response, this implementation always
	 * returns a result without one.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param headerCompleted
	 *            indicates that the message header has been completed and
	 *            the message (without body) is available
	 */
	@Override
	public Result newResult(boolean overflow, boolean underflow,
	        boolean headerCompleted) {
		return newResult(overflow, underflow, headerCompleted, null, false);
	}

	/**
	 * Factory method for result.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param headerCompleted {@code true} if the header has completely
	 * been decoded
	 * @param response a response to send due to an error
	 * @param responseOnly if the result includes a response 
	 * this flag indicates that no further processing besides 
	 * sending the response is required
	 * @return the result
	 */
	public Result newResult (boolean overflow, boolean underflow, 
			boolean headerCompleted, HttpResponse response, 
			boolean responseOnly) {
		return new Result(overflow, underflow, 
				headerCompleted, response, responseOnly) {
		};
	}

	/**
	 * Short for {@code RequestDecoder.Result<HttpResponse>}, provided
	 * for convenience.
	 * <P>
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 * 
	 * @author Michael N. Lipp
	 */
	public static abstract class Result 
		extends Decoder.Result<HttpResponse> {

		/**
		 * Creates a new result.
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param headerCompleted
		 *            {@code true} if the header has completely been decoded
		 * @param response
		 *            a response to send due to an error
		 * @param responseOnly
		 *            if the result includes a response this flag indicates that
		 *            no further processing besides sending the response is
		 *            required
		 */
		public Result(boolean overflow, boolean underflow,
		        boolean headerCompleted, HttpResponse response, 
		        boolean responseOnly) {
			super(overflow, underflow, false, headerCompleted, response,
					responseOnly);
		}
	}
}
