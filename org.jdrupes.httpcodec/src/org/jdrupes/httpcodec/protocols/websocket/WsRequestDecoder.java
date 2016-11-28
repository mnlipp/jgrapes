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
package org.jdrupes.httpcodec.protocols.websocket;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.RequestDecoder;
import org.jdrupes.httpcodec.protocols.http.HttpDecoder;

/**
 * @author Michael N. Lipp
 */
public class WsRequestDecoder extends WsDecoder
	implements RequestDecoder<WsFrameHeader, WsFrameHeader> {

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
	public Result decode(ByteBuffer in, Buffer out, boolean endOfInput)
	        throws ProtocolException {
		return (Result)super.decode(in, out, endOfInput);
	}

	/**
	 * Overrides the base interface's factory method in order to make
	 * it return the extended return type. As the {@link HttpDecoder}
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
	 * @param overflow
	 *            {@code true} if the data didn't fit in the out buffer
	 * @param underflow
	 *            {@code true} if more data is expected
	 * @param headerCompleted
	 *            {@code true} if the header has completely been decoded
	 * @param response
	 *            a response to send due to an error
	 * @param requestCompleted
	 *            if the result includes a response this flag indicates that
	 *            no further processing besides sending the response is
	 *            required
	 */
	public Result newResult (boolean overflow, boolean underflow, 
			boolean headerCompleted, WsFrameHeader response, 
			boolean requestCompleted) {
		return new Result(overflow, underflow, 
				headerCompleted, requestCompleted, response) {
		};
	}

	/**
	 * Short for {@code RequestDecoder.Result<WsFrameHeader>}, provided
	 * for convenience.
	 * <P>
	 * The class is declared abstract to promote the usage of the factory
	 * method.
	 * 
	 * @author Michael N. Lipp
	 */
	public static abstract class Result 
		extends RequestDecoder.Result<WsFrameHeader> {

		/**
		 * Creates a new result.
		 * @param overflow
		 *            {@code true} if the data didn't fit in the out buffer
		 * @param underflow
		 *            {@code true} if more data is expected
		 * @param headerCompleted
		 *            {@code true} if the header has completely been decoded
		 * @param requestCompleted
		 *            if the result includes a response this flag indicates that
		 *            no further processing besides sending the response is
		 *            required
		 * @param response
		 *            a response to send due to an error
		 */
		public Result(boolean overflow, boolean underflow,
		        boolean headerCompleted, boolean requestCompleted, 
		        WsFrameHeader response) {
			super(overflow, underflow, headerCompleted, response,
			        requestCompleted);
		}
	}
}
