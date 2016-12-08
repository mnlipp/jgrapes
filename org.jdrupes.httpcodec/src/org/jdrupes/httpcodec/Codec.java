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

import java.nio.ByteBuffer;

/**
 * @author Michael N. Lipp
 *
 */
public interface Codec {

	/**
	 * An empty input buffer that can be used for codec invocations
	 * when the (expected) body data is not yet available.
	 */
	public final static ByteBuffer EMPTY_IN = ByteBuffer.allocate(0);
	
	/**
	 * Factory method for results. 
	 * 
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param closeConnection
	 *            {@code true} if the connection should be closed
	 * @return the result
	 */
	default Result newResult (boolean overflow, boolean underflow,
			boolean closeConnection) {
		return new Result(overflow, underflow, closeConnection) {
		};
	}
	
	/**
	 * The common properties of the result types returned by the various codecs.
	 * <P>
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 * 
	 * @author Michael N. Lipp
	 */
	public static abstract class Result {

		private boolean overflow;
		private boolean underflow;
		private boolean closeConnection;

		/**
		 * Creates a new result with the given values.
		 * 
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 */
		protected Result(boolean overflow, boolean underflow,
				boolean closeConnection) {
			super();
			this.overflow = overflow;
			this.underflow = underflow;
			this.closeConnection = closeConnection;
		}

		/**
		 * Indicates that the data didn't fit in the out buffer. The encoding
		 * method that has returned this result should be re-invoked with the
		 * same parameters except for a new (or cleared) output buffer.
		 * 
		 * @return {@code true} if overflow occurred
		 * @see #isUnderflow()
		 */
		public boolean isOverflow() {
			return overflow;
		}

		/**
		 * Indicates that more data is needed to complete the encoding or
		 * decoding of the entity. 
		 * <P>
		 * {@code Codec}s may report an underflow 
		 * condition even if there is still data available in the input 
		 * buffer in order to report some special condition in an extended 
		 * result type. In this case, the encode or decode method should 
		 * be reinvoked with the same parameters after handling the special 
		 * condition that has been reported.
		 * <P>
		 * If underflow is reported and the input buffer is empty, the
		 * encode or decode method should be reinvoked with the same parameters
		 * except for an input buffer with additional information.
		 * <P>
		 * A result with both overflow and underflow set to false indicates
		 * the completion of the encoding or decoding process of the entity.
		 * In this case, the input buffer may still contain data that belongs
		 * to the next entity that is to be encoded or decoded. 
		 * 
		 * @return {@code true} if underflow occurred
		 */
		public boolean isUnderflow() {
			return underflow;
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

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (closeConnection ? 1231 : 1237);
			result = prime * result + (overflow ? 1231 : 1237);
			result = prime * result + (underflow ? 1231 : 1237);
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Result other = (Result) obj;
			if (closeConnection != other.closeConnection)
				return false;
			if (overflow != other.overflow)
				return false;
			if (underflow != other.underflow)
				return false;
			return true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Codec.Result [overflow=");
			builder.append(overflow);
			builder.append(", underflow=");
			builder.append(underflow);
			builder.append(", closeConnection=");
			builder.append(closeConnection);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Results that indicate a protocol switch must implement this interface.
	 * 
	 * @author Michael N. Lipp
	 */
	public interface ProtocolSwitchResult {
		
		/**
		 * The name of the protocol to be used for the next request
		 * if a protocol switch occured.
		 * 
		 * @return the name or {@code null} if no protocol switch occured
		 */
		public String newProtocol();
		
		/**
		 * The response encoder to be used for the next response
		 * if a protocol switch occured.
		 * 
		 * @return the encoder or {@code null} if no protocol switch occurred
		 */
		public Encoder<?> newEncoder();
		
		/**
		 * The request decoder to be used for the next request
		 * if a protocol switch occured.
		 * 
		 * @return the decoder or {@code null} if no protocol switch occurred
		 */
		public Decoder<?, ?> newDecoder();
	}
}
