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
package org.jdrupes.httpcodec.protocols.http.client;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.Engine;
import org.jdrupes.httpcodec.ResponseDecoder;
import org.jdrupes.httpcodec.Encoder;
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
	extends HttpDecoder<HttpResponse, HttpRequest>
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

	/**
	 * Overrides the base interface's factory method in order to make
	 * it return the extended return type. The value for 
	 * {@code closeConnection} is taken from {@link #isClosed()}.
	 * a protocol change is never reported by the base class.
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
	public Result newResult(boolean overflow,
	        boolean underflow, boolean headerCompleted) {
		return newResult (overflow, underflow, isClosed(), headerCompleted,
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
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws HttpProtocolException {
		return (Result)super.decode(in, out, endOfInput);
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

	/**
	 * Factory method for result.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param closeConnection
	 *            {@code true} if the connection should be closed
	 * @param headerCompleted {@code true} if the header has completely
	 * been decoded
	 * @param newProtocol the name of the new protocol if a switch occurred
	 * @param newDecoder the new decoder if a switch occurred
	 * @param newEncoder the new decoder if a switch occurred
	 * @return the result
	 */
	public Result newResult (boolean overflow, boolean underflow, 
			boolean closeConnection, boolean headerCompleted,
	        String newProtocol, ResponseDecoder<?,?> newDecoder, 
	        Encoder<?> newEncoder) {
		return new Result(overflow, underflow, closeConnection,
				headerCompleted, newProtocol, newDecoder, newEncoder) {
		};
	}

	/**
	 * Overrides the base interface's factory method in order to make
	 * it return the extended return type.
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param closeConnection
	 *            {@code true} if the connection should be closed
	 * @param headerCompleted {@code true} if the header has completely
	 * been decoded
	 * @param response a response to send due to an error
	 * @param responseOnly if the result includes a response 
	 * 	this flag indicates that no further processing besides 
	 * 	sending the response is required
	 * @return the result
	 */
	public Result newResult (boolean overflow, boolean underflow, 
			boolean closeConnection, boolean headerCompleted, 
			HttpRequest response, boolean responseOnly) {
		return newResult(overflow, underflow, closeConnection,
				headerCompleted, null, null, null);
	}
	
	/**
	 * The result from encoding a response. In addition to the usual
	 * codec result, a result decoder may signal to the invoker that the
	 * connection to the responder must be closed.
	 * <P>
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 * 
	 * @author Michael N. Lipp
	 */
	public abstract class Result extends Decoder.Result<HttpRequest>
		implements Codec.ProtocolSwitchResult {

		private String newProtocol;
		private ResponseDecoder<?, ?> newDecoder;
		private Encoder<?> newEncoder;
		
		/**
		 * Returns a new result.
		 * 
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 * @param newProtocol the name of the new protocol if a switch occurred
		 * @param newDecoder the new decoder if a switch occurred
		 * @param newEncoder the new decoder if a switch occurred
		 */
		protected Result(boolean overflow, boolean underflow,
		        boolean closeConnection, boolean headerCompleted,
		        String newProtocol, ResponseDecoder<?, ?> newDecoder, 
		        Encoder<?> newEncoder) {
			super(overflow, underflow, closeConnection, headerCompleted,
					null, false);
			this.newProtocol = newProtocol;
			this.newDecoder = newDecoder;
			this.newEncoder = newEncoder;
		}

		@Override
		public String newProtocol() {
			return newProtocol;
		}
		
		@Override
		public ResponseDecoder<?, ?> newDecoder() {
			return newDecoder;
		}
		
		@Override
		public Encoder<?> newEncoder() {
			return newEncoder;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result
			        + ((newDecoder == null) ? 0 : newDecoder.hashCode());
			result = prime * result
			        + ((newEncoder == null) ? 0 : newEncoder.hashCode());
			result = prime * result
			        + ((newProtocol == null) ? 0 : newProtocol.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			Result other = (Result) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (newDecoder == null) {
				if (other.newDecoder != null)
					return false;
			} else if (!newDecoder.equals(other.newDecoder))
				return false;
			if (newEncoder == null) {
				if (other.newEncoder != null)
					return false;
			} else if (!newEncoder.equals(other.newEncoder))
				return false;
			if (newProtocol == null) {
				if (other.newProtocol != null)
					return false;
			} else if (!newProtocol.equals(other.newProtocol))
				return false;
			return true;
		}

		private HttpResponseDecoder getOuterType() {
			return HttpResponseDecoder.this;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("HttpResponseDecoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(getCloseConnection());
			builder.append(", headerCompleted=");
			builder.append(isHeaderCompleted());
			builder.append(", ");
			if (getResponse() != null) {
				builder.append("response=");
				builder.append(getResponse());
				builder.append(", ");
			}
			builder.append("responseOnly=");
			builder.append(isResponseOnly());
			builder.append(", ");
			if (newProtocol != null) {
				builder.append("newProtocol=");
				builder.append(newProtocol);
				builder.append(", ");
			}
			if (newDecoder != null) {
				builder.append("newDecoder=");
				builder.append(newDecoder);
				builder.append(", ");
			}
			if (newEncoder != null) {
				builder.append("newEncoder=");
				builder.append(newEncoder);
			}
			builder.append("]");
			return builder.toString();
		}
		
	}
}
