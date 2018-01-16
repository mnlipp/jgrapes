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

package org.jgrapes.io.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A specialized {@code ManagedBuffer<ByteBuffer>} that provides
 * the same methods as {@code ByteBuffer} by delegating to the
 * backing buffer.
 */
public class ManagedByteBuffer extends ManagedBuffer<ByteBuffer> {

	/**
	 * An empty buffer usable instead of {@code null}.
	 */
	public static final ManagedByteBuffer EMPTY_BUFFER 
		= new ManagedByteBuffer(ByteBuffer.allocate(0), 
				BufferCollector.NOOP_COLLECTOR);
	
	/**
	 * Creates new managed buffer that is backed by the given buffer
	 * and managed by the given manager. 
	 * 
	 * @param buffer the backing buffer
	 * @param manager the manager
	 */
	public ManagedByteBuffer(ByteBuffer buffer, BufferCollector manager) {
		super(buffer, manager);
	}

	/**
	 * Creates new managed buffer that is backed by a {@link ByteBuffer}
	 * that wraps the given array.
	 * 
	 * @param array the byte array
	 */
	public ManagedByteBuffer(byte[] array) {
		super(ByteBuffer.wrap(array), BufferCollector.NOOP_COLLECTOR);
	}
	
	/**
	 * Creates new managed buffer that is backed by a {@link ByteBuffer}
	 * that wraps the given sub array.
	 * 
	 * @param array the byte array
	 * @param offset the offset of the sub array to be wrapped
	 * @param length the length of the sub array to be wrapped
	 */
	public ManagedByteBuffer(byte[] array, int offset, int length) {
		super(ByteBuffer.wrap(array, offset, length), BufferCollector.NOOP_COLLECTOR);
	}

	/**
	 * Creates a new {@link Reader}. 
	 * 
	 * @return the reader
	 */
	public Reader newReader() {
		return new Reader();
	}
	
	/**
	 * Convenience method to fill the buffer from the channel.
	 * Unlocks the buffer if an {@link IOException} occurs.
	 *
	 * @param channel the channel
	 * @return the bytes read
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public int fillFromChannel(
			ReadableByteChannel channel) throws IOException {
		try {
			return channel.read(backingBuffer());
		} catch (IOException e) {
			unlockBuffer();
			throw e;
		}
	}
	
	/**
	 * @see java.nio.ByteBuffer#array()
	 */
	@Override
	public final byte[] array() {
		return backing.array();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asCharBuffer()
	 */
	public CharBuffer asCharBuffer() {
		return backing.asCharBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asDoubleBuffer()
	 */
	public DoubleBuffer asDoubleBuffer() {
		return backing.asDoubleBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asFloatBuffer()
	 */
	public FloatBuffer asFloatBuffer() {
		return backing.asFloatBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asIntBuffer()
	 */
	public IntBuffer asIntBuffer() {
		return backing.asIntBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asLongBuffer()
	 */
	public LongBuffer asLongBuffer() {
		return backing.asLongBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asReadOnlyBuffer()
	 */
	public ByteBuffer asReadOnlyBuffer() {
		return backing.asReadOnlyBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#asShortBuffer()
	 */
	public ShortBuffer asShortBuffer() {
		return backing.asShortBuffer();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#compact()
	 */
	public ByteBuffer compact() {
		return backing.compact();
	}

	/**
	 * @param that the buffer to compare to
	 * @return the result
	 * @see java.nio.ByteBuffer#compareTo(java.nio.ByteBuffer)
	 */
	public int compareTo(ByteBuffer that) {
		return backing.compareTo(that);
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#duplicate()
	 */
	public ByteBuffer duplicate() {
		return backing.duplicate();
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#order()
	 */
	public final ByteOrder order() {
		return backing.order();
	}

	/**
	 * @param bo the byte order
	 * @return the result
	 * @see java.nio.ByteBuffer#order(java.nio.ByteOrder)
	 */
	public final ByteBuffer order(ByteOrder bo) {
		return backing.order(bo);
	}

	/**
	 * @param data the byte
	 * @return the result
	 * @see java.nio.ByteBuffer#put(byte)
	 */
	public ByteBuffer put(byte data) {
		return backing.put(data);
	}

	/**
	 * @param src the array of bytes
	 * @param offset the beginning of the region
	 * @param length the size of the region
	 * @return the result
	 * @see java.nio.ByteBuffer#put(byte[], int, int)
	 */
	public ByteBuffer put(byte[] src, int offset, int length) {
		return backing.put(src, offset, length);
	}

	/**
	 * @param src the array of bytes
	 * @return the result
	 * @see java.nio.ByteBuffer#put(byte[])
	 */
	public final ByteBuffer put(byte[] src) {
		return backing.put(src);
	}

	/**
	 * @param src the buffer to use as source
	 * @return the result
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ByteBuffer src) {
		return backing.put(src);
	}

	/**
	 * @param src the buffer to use as source
	 * @return the result
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ManagedByteBuffer src) {
		return backing.put(src.backingBuffer());
	}

	/**
	 * @param index the index
	 * @param data the byte
	 * @return the result
	 * @see java.nio.ByteBuffer#put(int, byte)
	 */
	public ByteBuffer put(int index, byte data) {
		return backing.put(index, data);
	}

	/**
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putChar(char)
	 */
	public ByteBuffer putChar(char value) {
		return backing.putChar(value);
	}

	/**
	 * @param index the index
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putChar(int, char)
	 */
	public ByteBuffer putChar(int index, char value) {
		return backing.putChar(index, value);
	}

	/**
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putDouble(double)
	 */
	public ByteBuffer putDouble(double value) {
		return backing.putDouble(value);
	}

	/**
	 * @param index the index
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putDouble(int, double)
	 */
	public ByteBuffer putDouble(int index, double value) {
		return backing.putDouble(index, value);
	}

	/**
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putFloat(float)
	 */
	public ByteBuffer putFloat(float value) {
		return backing.putFloat(value);
	}

	/**
	 * @param index the index
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putFloat(int, float)
	 */
	public ByteBuffer putFloat(int index, float value) {
		return backing.putFloat(index, value);
	}

	/**
	 * @param index the index
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putInt(int, int)
	 */
	public ByteBuffer putInt(int index, int value) {
		return backing.putInt(index, value);
	}

	/**
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putInt(int)
	 */
	public ByteBuffer putInt(int value) {
		return backing.putInt(value);
	}

	/**
	 * @param index the index
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putLong(int, long)
	 */
	public ByteBuffer putLong(int index, long value) {
		return backing.putLong(index, value);
	}

	/**
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putLong(long)
	 */
	public ByteBuffer putLong(long value) {
		return backing.putLong(value);
	}

	/**
	 * @param index the index
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putShort(int, short)
	 */
	public ByteBuffer putShort(int index, short value) {
		return backing.putShort(index, value);
	}

	/**
	 * @param value the value
	 * @return the result
	 * @see java.nio.ByteBuffer#putShort(short)
	 */
	public ByteBuffer putShort(short value) {
		return backing.putShort(value);
	}

	/**
	 * @return the result
	 * @see java.nio.ByteBuffer#slice()
	 */
	public ByteBuffer slice() {
		return backing.slice();
	}

	/**
	 * A reader for the buffers content. The reader consists
	 * of a read only view of the managed buffer's content
	 * (backing buffer) and a reference to the managed buffer.
	 */
	public class Reader {
		private ByteBuffer bufferView;
		
		private Reader() {
			bufferView = backingBuffer().asReadOnlyBuffer();
		}

		/**
		 * Returns the {@link ByteBuffer} that represents this
		 * view (position, mark, limit).
		 * 
		 * @return the `ByteBuffer` view
		 */
		public ByteBuffer get() {
			return bufferView;
		}

		/**
		 * Returns the managed buffer that this reader is a view of.
		 * 
		 * @return the managed buffer
		 */
		public ManagedByteBuffer managedBuffer() {
			return ManagedByteBuffer.this;
		}
	}
}
