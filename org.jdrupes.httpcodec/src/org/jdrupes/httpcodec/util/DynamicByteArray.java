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

/**
 * A dynamically growing byte array.
 * 
 * @author Michael N. Lipp
 */
public class DynamicByteArray {

	private byte[] bytes;
	private int position;

	/**
	 * Creates the array with the given initial size.
	 * 
	 * @param initialSize the initial size
	 */
	public DynamicByteArray(int initialSize) {
		if (initialSize < 128) {
			initialSize = 128;
		}
		bytes = new byte[initialSize];
		position = 0;
	}

	/**
	 * Appends the given byte, growing the array if necessary.
	 * 
	 * @param b the byte to append
	 */
	public void append(byte b) {
		if (position >= bytes.length) {
			byte[] newBytes = new byte[(int)(bytes.length * 1.3)];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			bytes = newBytes;
		}
		bytes[position++] = b;
	}

	/**
	 * Appends the given bytes, growing the array if necessary.
	 * 
	 * @param b an array of bytes
	 * @param offset the first byte to append
	 * @param length the number of bytes to append
	 */
	public void append(byte[] b, int offset, int length) {
		if (bytes.length - position < length) {
			byte[] newBytes = new byte[(int)((bytes.length + length) * 1.3)];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			bytes = newBytes;
		}
		System.arraycopy(b, offset, bytes, position, length);
	}

	/**
	 * Returns the current position (number of bytes in the array).
	 * 
	 * @return the position
	 */
	public int position() {
		return position;
	}
	
	/**
	 * Resets the position to 0. 
	 */
	public void clear() {
		position = 0;
	}
	
	/**
	 * Returns the internal storage for the bytes. 
	 * 
	 * @return the storage
	 */
	public byte[] array() {
		return bytes;
	}
}
