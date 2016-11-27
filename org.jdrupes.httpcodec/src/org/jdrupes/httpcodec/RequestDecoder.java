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

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * A decoder that is used to encode a request. This type of decoder has
 * an extended result type.
 *
 * @param <Q> the type of the message header to be decoded (the request)
 * @param <R> the type of the optionally generated response message header
 * 
 * @author Michael N. Lipp
 */
public interface RequestDecoder<Q extends MessageHeader, 
	R extends MessageHeader> extends Decoder<Q> {

	/**
	 * Decodes the next chunk of data.
	 * 
	 * @param in
	 *            holds the data to be decoded
	 * @param out
	 *            gets the body data (if any) written to it
	 * @param endOfInput
	 *            {@code true} if there is no input left beyond the data
	 *            currently in the {@code in} buffer (indicates end of body or
	 *            no body at all)
	 * @return the result
	 * @throws ProtocolException
	 *             if the message violates the HTTP
	 */
	@Override
	public Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException;

	/**
	 * The result from decoding a request. In addition to the common
	 * decoder result, this can include a response that is to be sent
	 * back to the requester in order to fulfill the requirements of
	 * the protocol. As the decoder can (obviously) not sent back this
	 * response by itself, it is included in the result.
	 *
	 * @param <R> the type of the optionally generated response message
	 * @author Michael N. Lipp
	 */
	public static class Result<R extends MessageHeader> 
		extends Decoder.Result {

		private R response;
		private boolean requestComleted;

		/**
		 * Creates a new result.
		 * 
		 * @param headerCompleted {@code true} if the header has completely
		 * been decoded
		 * @param response a response to send due to an error
		 * @param requestCompleted if the result includes a response 
		 * this flag indicates that no further processing besides 
		 * sending the response is required
		 * @param overflow {@code true} if the data didn't fit in the out buffer
		 * @param underflow {@code true} if more data is expected
		 */
		public Result(boolean headerCompleted, R response, 
				boolean requestCompleted, boolean overflow, 
				boolean underflow) {
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
		 * the decoder result indicates that some information
		 * must be signaled back to the client.
		 * 
		 * @return the response
		 */
		public R getResponse() {
			return response;
		}

		/**
		 * If the result includes a response ({@link #hasResponse()} is
		 * {@code true}) and this method returns {@code true} then no
		 * further processing of the request (besides sending the response)
		 * is required.  
		 * 
		 * @return the result
		 */
		public boolean requestCompleted() {
			return requestComleted;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("RequestDecoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", headerCompleted=");
			builder.append(isHeaderCompleted());
			builder.append(", ");
			if (response != null) {
				builder.append("response=");
				builder.append(response);
				builder.append(", ");
			}
			builder.append("requestComleted=");
			builder.append(requestComleted);
			builder.append("]");
			return builder.toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (requestComleted ? 1231 : 1237);
			result = prime * result
			        + ((response == null) ? 0 : response.hashCode());
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
			@SuppressWarnings("unchecked")
			Result<R> other = (Result<R>) obj;
			if (requestComleted != other.requestComleted)
				return false;
			if (response == null) {
				if (other.response != null)
					return false;
			} else if (!response.equals(other.response))
				return false;
			return true;
		}
		
		
	}
	
}
