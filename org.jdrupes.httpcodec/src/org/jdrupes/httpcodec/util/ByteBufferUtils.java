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
package org.jdrupes.httpcodec.util;

import java.nio.ByteBuffer;

/**
 * @author Michael N. Lipp
 *
 */
public class ByteBufferUtils {

	private ByteBufferUtils() {
	}

	/**
	 * Put as many bytes as possible from the src buffer into the destination
	 * buffer.
	 * 
	 * @param dest
	 *            the destination buffer
	 * @param src
	 *            the source buffer
	 * @return {@code true} if {@code src.remaining() == 0}
	 */
	public static boolean putAsMuchAsPossible(ByteBuffer dest, ByteBuffer src) {
		if (dest.remaining() >= src.remaining()) {
			dest.put(src);
			return true;
		}
		if (dest.remaining() > 0) {
			int oldLimit = src.limit();
			src.limit(src.position() + dest.remaining());
			dest.put(src);
			src.limit(oldLimit);
		}
		return false;
	}

	/**
	 * Put as many bytes as possible from the src buffer into the destination
	 * buffer but not more than specified by limit.
	 * 
	 * @param dest
	 *            the destination buffer
	 * @param src
	 *            the source buffer
	 * @param limit
	 *            the maximum number of bytes to transfer
	 * @return {@code true} if {@code src.remaining() == 0}
	 */
	public static boolean putAsMuchAsPossible(ByteBuffer dest, ByteBuffer src,
	        int limit) {
		if (src.remaining() <= limit) {
			return putAsMuchAsPossible(dest, src);
		}
		int oldLimit = src.limit();
		try {
			src.limit(src.position() + limit);
			return putAsMuchAsPossible(dest, src);
		} finally {
			src.limit(oldLimit);
		}
	}

}
