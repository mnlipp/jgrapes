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
import java.util.Optional;

/**
 * The general interface of a decoder.
 * 
 * @param <T> the type of message decoded by this decoder
 * 
 * @author Michael N. Lipp
 *
 */
public interface Decoder<T extends MessageHeader> extends Codec {

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
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
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
	 * @param headerCompleted
	 *            indicates that the message header has been completed and
	 *            the message (without body) is available
	 */
	default Result newResult (boolean overflow, boolean underflow, 
			boolean headerCompleted) {
		return new Result (overflow, underflow, headerCompleted) {
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
	 */
	Result newResult (boolean overflow, boolean underflow);
	
	/**
	 * The result from invoking the decoder. In addition to the general codec
	 * result, all decoders may return a message header if one
	 * has been decoded during the invocation. 
	 * <P>
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 * 
	 * @author Michael N. Lipp
	 */
	public abstract class Result extends Codec.Result {

		private boolean headerCompleted;

		/**
		 * Creates a new result.
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 */
		protected Result(boolean overflow, boolean underflow, 
				boolean headerCompleted) {
			super(overflow, underflow);
			this.headerCompleted = headerCompleted;
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
			builder.append(", headerCompleted=");
			builder.append(headerCompleted);
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
			result = prime * result + (headerCompleted ? 1231 : 1237);
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
			if (headerCompleted != other.headerCompleted)
				return false;
			return true;
		}
	}
}
