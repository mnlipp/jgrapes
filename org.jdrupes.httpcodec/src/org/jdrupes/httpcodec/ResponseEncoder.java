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
	 * connection to the requester must be closed.
	 * 
	 * @author Michael N. Lipp
	 */
	public class Result extends Codec.Result {

		boolean closeConnection;
		
		/**
		 * Returns a new result.
		 * 
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param closeConnection
		 *            {@code true} if the connection should be closed
		 */
		public Result(boolean overflow, boolean underflow,
		        boolean closeConnection) {
			super(overflow, underflow);
			this.closeConnection = closeConnection;
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
	}

	
}
