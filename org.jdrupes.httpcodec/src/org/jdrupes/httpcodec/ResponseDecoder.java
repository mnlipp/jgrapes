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

/**
 * A decoder that is used to decode a response. This type of decoder 
 * provides an additional method.
 * 
 * @param <T>
 *            the type of the message header to be decoded (the response)
 * @param <Q>
 *            the type of the message header that caused the response (the
 *            request)
 * 
 * @author Michael N. Lipp
 */
public interface ResponseDecoder<T extends MessageHeader, 
	Q extends MessageHeader> extends Decoder<T, Q> {

	/**
	 * Causes the decoder to interpret the data in invocations of
	 * {@link #decode(ByteBuffer, Buffer, boolean)} as response to the given
	 * request header. Some protocols need information from the previously 
	 * sent request in order to interpret the response correctly.
	 * <P>
	 * Must be invoked before the first invocation of
	 * {@link #decode(ByteBuffer, Buffer, boolean)} for a given response.
	 * 
	 * @param request
	 *            the request header
	 */
	public void decodeResponseTo(Q request);
}
