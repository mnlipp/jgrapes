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
	 * or {@code null} if none has been received yet.
	 * 
	 * @return the result
	 */
	public T getHeader();
	
	/**
	 * The result from invoking the decoder. In addition to the general codec
	 * result, all decoders may return a message header if one
	 * has been decoded during the invocation. 
	 * 
	 * @author Michael N. Lipp
	 */
	public abstract class Result extends Codec.Result {

		private boolean headerCompleted;

		/**
		 * Creates a new result.
		 * 
		 * @param headerCompleted
		 *            indicates that the message header has been completed and
		 *            the message (without body) is available
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 */
		protected Result(boolean headerCompleted, boolean overflow, 
				boolean underflow) {
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
		
	}
}
