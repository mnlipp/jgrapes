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
 * An encoder that encodes a response. This kind of encoder has an
 * extended result type. 
 * 
 * @author Michael N. Lipp
 */
public interface ResponseEncoder<T extends MessageHeader>
	extends Encoder<T> {

	/**
	 * Set the header of the message that is to be encoded. Must be invoked
	 * before the first invocation to any {@code encode} method for a given
	 * message.
	 * 
	 * @param messageHeader
	 *            the message header
	 */
	public void encode(T messageHeader);
	
	/**
	 * Encodes a message.
	 * 
	 * @param in
	 *            the body data
	 * @param out
	 *            the buffer to which data is written
	 * @param endOfInput
	 *            {@code true} if there is no input left beyond the data
	 *            currently in the {@code in} buffer (indicates end of body or
	 *            no body at all)
	 * @return the result
	 */
	@Override
	public Result encode(Buffer in, ByteBuffer out,
	        boolean endOfInput)	;

	/**
	 * Convenience method for invoking
	 * {@link #encode(Buffer, ByteBuffer, boolean)} with an empty {@code in}
	 * buffer and {@code true}. Can be used to get the result of encoding a 
	 * message without body.
	 * 
	 * @param out
	 *            the buffer to which data is written
	 * @return the result
	 */
	@Override
	default public Result encode(ByteBuffer out) {
		return encode(EMPTY_IN, out, true);
	}

	/**
	 * The result from encoding a response. In addition to the usual
	 * codec result, a result encoder may signal to the invoker that the
	 * connection to the requester must be closed and that the protocol has
	 * been switched.
	 * 
	 * @author Michael N. Lipp
	 */
	public class Result extends Codec.Result {

		private boolean closeConnection;
		private String newProtocol;
		private RequestDecoder<? extends MessageHeader, 
				? extends MessageHeader> newDecoder;
		private ResponseEncoder<? extends MessageHeader> newEncoder;
		
		/**
		 * Returns a new result.
		 * 
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 * @param newProtocol the name of the new protocol if a switch occurred
		 * @param newDecoder the new decoder if a switch occurred
		 * @param newEncoder the new decoder if a switch occurred
		 */
		public Result(boolean overflow, boolean underflow,
		        boolean closeConnection, String newProtocol,
		        RequestDecoder<? extends MessageHeader, 
		        		? extends MessageHeader> newDecoder,
		        ResponseEncoder<? extends MessageHeader> newEncoder) {
			super(overflow, underflow);
			this.closeConnection = closeConnection;
			this.newProtocol = newProtocol;
			this.newEncoder = newEncoder;
			this.newDecoder = newDecoder;
		}

		/**
		 * Indicates that the connection to the receiver of the response must be
		 * closed after sending any remaining data in the out buffer.
		 * 
		 * @return the value
		 */
		public boolean getCloseConnection() {
			return closeConnection;
		}

		/**
		 * The name of the protocol to be used for the next request
		 * if a protocol switch occured.
		 * 
		 * @return the name or {@code null} if no protocol switch occured
		 */
		public String newProtocol() {
			return newProtocol;
		}
		
		/**
		 * The response encoder to be used for the next response
		 * if a protocol switch occured.
		 * 
		 * @return the encoder or {@code null} if no protocol switch occurred
		 */
		public ResponseEncoder<? extends MessageHeader> newEncoder() {
			return newEncoder;
		}
		
		/**
		 * The request decoder to be used for the next request
		 * if a protocol switch occured.
		 * 
		 * @return the decoder or {@code null} if no protocol switch occurred
		 */
		public RequestDecoder<? extends MessageHeader,
				? extends MessageHeader> newDecoder() {
			return newDecoder;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ResponseEncoder.Result [overflow=");
			builder.append(isOverflow());
			builder.append(", underflow=");
			builder.append(isUnderflow());
			builder.append(", closeConnection=");
			builder.append(closeConnection);
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

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (closeConnection ? 1231 : 1237);
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
			if (closeConnection != other.closeConnection)
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
	}
}
