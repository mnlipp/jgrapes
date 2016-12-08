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
package org.jdrupes.httpcodec;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * The general interface of a decoder.
 * 
 * @param <T> the type of message decoded by this decoder
 * @param <R> the type of message that may be generated as response
 * (see {@link Decoder.Result#getResponse()})
 * 
 * @author Michael N. Lipp
 *
 */
public interface Decoder<T extends MessageHeader,
	R extends MessageHeader> extends Codec {

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
	public Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException;
	
	/**
	 * Returns the last message (header) received completely 
	 * if it exists.
	 * 
	 * @return the result
	 */
	public Optional<T> getHeader();
	
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
	 * @param response a response to send due to an error
	 * @param responseOnly if the result includes a response 
	 * this flag indicates that no further processing besides 
	 * sending the response is required
	 * @return the result
	 */
	default Result<R> newResult (boolean overflow, boolean underflow, 
			boolean closeConnection, boolean headerCompleted, 
			R response, boolean responseOnly) {
		return new Result<R>(overflow, underflow, closeConnection,
				headerCompleted, response, responseOnly) {
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
	 * @param headerCompleted
	 *            indicates that the message header has been completed and
	 *            the message (without body) is available
	 */
	Result<R> newResult (boolean overflow, boolean underflow, 
			boolean headerCompleted);
	
	/**
	 * The result from decoding. In addition to the common codec result, this
	 * includes the information wheteher a complete message header has been
	 * received and it can include a response that is to be sent back to the
	 * sender in order to fulfill the requirements of the protocol. As the
	 * decoder can (obviously) not sent back this response by itself, it is
	 * included in the result.
	 * <P>
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 *
	 * @param <R>
	 *            the type of the optionally generated response message
	 * @author Michael N. Lipp
	 */
	public static abstract class Result<R extends MessageHeader> 
		extends Codec.Result {

		private boolean headerCompleted;
		private R response;
		private boolean responseOnly;

		/**
		 * Creates a new result.
		 * @param overflow {@code true} if the data didn't fit in the out buffer
		 * @param underflow {@code true} if more data is expected
		 * @param closeConnection
		 * 	 {@code true} if the connection should be closed
		 * @param headerCompleted {@code true} if the header has completely
		 * been decoded
		 * @param response a response to send due to an error
		 * @param responseOnly if the result includes a response 
		 * this flag indicates that no further processing besides 
		 * sending the response is required
		 */
		protected Result(boolean overflow, boolean underflow, 
				boolean closeConnection, boolean headerCompleted, 
				R response, boolean responseOnly) {
			super(overflow, underflow, closeConnection);
			this.headerCompleted =  headerCompleted;
			this.response = response;
			this.responseOnly = responseOnly;
		}

		/**
		 * Returns {@code true} if the message header has been decoded 
		 * completely during the decoder invocation that returned this 
		 * result and is now available. 
		 * 
		 * @return the result
		 */
		public boolean isHeaderCompleted() {
			return headerCompleted;
		}

		/**
		 * Returns {@code true} if the result includes a response
		 * (see {@link #getResponse()}).
		 * 
		 * @return the result
		 */
		public boolean hasResponse() {
			return response != null;
		}
		
		/**
		 * Returns the response if a response exists. A response in
		 * the decoder result indicates that some information
		 * must be signaled back to the sender.
		 * 
		 * @return the response
		 */
		public R getResponse() {
			return response;
		}

		/**
		 * If the result includes a response ({@link #hasResponse()} returns
		 * {@code true}) and this method returns {@code true} then no
		 * further processing of the received data is required. After sending
		 * the response data, the decode method should be invoked 
		 * again with the same parameters. 
		 * 
		 * @return the result
		 */
		public boolean isResponseOnly() {
			return responseOnly;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (headerCompleted ? 1231 : 1237);
			result = prime * result + (responseOnly ? 1231 : 1237);
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
			Result<?> other = (Result<?>) obj;
			if (headerCompleted != other.headerCompleted)
				return false;
			if (responseOnly != other.responseOnly)
				return false;
			if (response == null) {
				if (other.response != null)
					return false;
			} else if (!response.equals(other.response))
				return false;
			return true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Decoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(getCloseConnection());
			builder.append(", headerCompleted=");
			builder.append(headerCompleted);
			builder.append(", ");
			if (response != null) {
				builder.append("response=");
				builder.append(response);
				builder.append(", ");
			}
			builder.append("requestComleted=");
			builder.append(responseOnly);
			builder.append("]");
			return builder.toString();
		}

	}
	
}
