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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * A specialized {@code ManagedBuffer<ByteBuffer>} that provides
 * the same methods as {@code ByteBuffer} by delegating to the
 * backing buffer.
 * 
 * @author Michael N. Lipp
 */
public class ManagedByteBuffer extends ManagedBuffer<ByteBuffer> {

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
	 * @see java.nio.ByteBuffer#array()
	 */
	@Override
	public final byte[] array() {
		return backing.array();
	}

	/**
	 * @see java.nio.ByteBuffer#asCharBuffer()
	 */
	public CharBuffer asCharBuffer() {
		return backing.asCharBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#asDoubleBuffer()
	 */
	public DoubleBuffer asDoubleBuffer() {
		return backing.asDoubleBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#asFloatBuffer()
	 */
	public FloatBuffer asFloatBuffer() {
		return backing.asFloatBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#asIntBuffer()
	 */
	public IntBuffer asIntBuffer() {
		return backing.asIntBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#asLongBuffer()
	 */
	public LongBuffer asLongBuffer() {
		return backing.asLongBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#asReadOnlyBuffer()
	 */
	public ByteBuffer asReadOnlyBuffer() {
		return backing.asReadOnlyBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#asShortBuffer()
	 */
	public ShortBuffer asShortBuffer() {
		return backing.asShortBuffer();
	}

	/**
	 * @see java.nio.ByteBuffer#compact()
	 */
	public ByteBuffer compact() {
		return backing.compact();
	}

	/**
	 * @see java.nio.ByteBuffer#compareTo(java.nio.ByteBuffer)
	 */
	public int compareTo(ByteBuffer that) {
		return backing.compareTo(that);
	}

	/**
	 * @see java.nio.ByteBuffer#duplicate()
	 */
	public ByteBuffer duplicate() {
		return backing.duplicate();
	}

	/**
	 * @see java.nio.ByteBuffer#get()
	 */
	public byte get() {
		return backing.get();
	}

	/**
	 * @see java.nio.ByteBuffer#get(byte[], int, int)
	 */
	public ByteBuffer get(byte[] dst, int offset, int length) {
		return backing.get(dst, offset, length);
	}

	/**
	 * @see java.nio.ByteBuffer#get(byte[])
	 */
	public ByteBuffer get(byte[] dst) {
		return backing.get(dst);
	}

	/**
	 * @see java.nio.ByteBuffer#get(int)
	 */
	public byte get(int index) {
		return backing.get(index);
	}

	/**
	 * @see java.nio.ByteBuffer#getChar()
	 */
	public char getChar() {
		return backing.getChar();
	}

	/**
	 * @see java.nio.ByteBuffer#getChar(int)
	 */
	public char getChar(int index) {
		return backing.getChar(index);
	}

	/**
	 * @see java.nio.ByteBuffer#getDouble()
	 */
	public double getDouble() {
		return backing.getDouble();
	}

	/**
	 * @see java.nio.ByteBuffer#getDouble(int)
	 */
	public double getDouble(int index) {
		return backing.getDouble(index);
	}

	/**
	 * @see java.nio.ByteBuffer#getFloat()
	 */
	public float getFloat() {
		return backing.getFloat();
	}

	/**
	 * @see java.nio.ByteBuffer#getFloat(int)
	 */
	public float getFloat(int index) {
		return backing.getFloat(index);
	}

	/**
	 * @see java.nio.ByteBuffer#getInt()
	 */
	public int getInt() {
		return backing.getInt();
	}

	/**
	 * @see java.nio.ByteBuffer#getInt(int)
	 */
	public int getInt(int index) {
		return backing.getInt(index);
	}

	/**
	 * @see java.nio.ByteBuffer#getLong()
	 */
	public long getLong() {
		return backing.getLong();
	}

	/**
	 * @see java.nio.ByteBuffer#getLong(int)
	 */
	public long getLong(int index) {
		return backing.getLong(index);
	}

	/**
	 * @see java.nio.ByteBuffer#getShort()
	 */
	public short getShort() {
		return backing.getShort();
	}

	/**
	 * @see java.nio.ByteBuffer#getShort(int)
	 */
	public short getShort(int index) {
		return backing.getShort(index);
	}

	/**
	 * @see java.nio.ByteBuffer#order()
	 */
	public final ByteOrder order() {
		return backing.order();
	}

	/**
	 * @see java.nio.ByteBuffer#order(java.nio.ByteOrder)
	 */
	public final ByteBuffer order(ByteOrder bo) {
		return backing.order(bo);
	}

	/**
	 * @see java.nio.ByteBuffer#put(byte)
	 */
	public ByteBuffer put(byte b) {
		return backing.put(b);
	}

	/**
	 * @see java.nio.ByteBuffer#put(byte[], int, int)
	 */
	public ByteBuffer put(byte[] src, int offset, int length) {
		return backing.put(src, offset, length);
	}

	/**
	 * @see java.nio.ByteBuffer#put(byte[])
	 */
	public final ByteBuffer put(byte[] src) {
		return backing.put(src);
	}

	/**
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ByteBuffer src) {
		return backing.put(src);
	}

	/**
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ManagedByteBuffer src) {
		return backing.put(src.getBacking());
	}

	/**
	 * @see java.nio.ByteBuffer#put(int, byte)
	 */
	public ByteBuffer put(int index, byte b) {
		return backing.put(index, b);
	}

	/**
	 * @see java.nio.ByteBuffer#putChar(char)
	 */
	public ByteBuffer putChar(char value) {
		return backing.putChar(value);
	}

	/**
	 * @see java.nio.ByteBuffer#putChar(int, char)
	 */
	public ByteBuffer putChar(int index, char value) {
		return backing.putChar(index, value);
	}

	/**
	 * @see java.nio.ByteBuffer#putDouble(double)
	 */
	public ByteBuffer putDouble(double value) {
		return backing.putDouble(value);
	}

	/**
	 * @see java.nio.ByteBuffer#putDouble(int, double)
	 */
	public ByteBuffer putDouble(int index, double value) {
		return backing.putDouble(index, value);
	}

	/**
	 * @see java.nio.ByteBuffer#putFloat(float)
	 */
	public ByteBuffer putFloat(float value) {
		return backing.putFloat(value);
	}

	/**
	 * @see java.nio.ByteBuffer#putFloat(int, float)
	 */
	public ByteBuffer putFloat(int index, float value) {
		return backing.putFloat(index, value);
	}

	/**
	 * @see java.nio.ByteBuffer#putInt(int, int)
	 */
	public ByteBuffer putInt(int index, int value) {
		return backing.putInt(index, value);
	}

	/**
	 * @see java.nio.ByteBuffer#putInt(int)
	 */
	public ByteBuffer putInt(int value) {
		return backing.putInt(value);
	}

	/**
	 * @see java.nio.ByteBuffer#putLong(int, long)
	 */
	public ByteBuffer putLong(int index, long value) {
		return backing.putLong(index, value);
	}

	/**
	 * @see java.nio.ByteBuffer#putLong(long)
	 */
	public ByteBuffer putLong(long value) {
		return backing.putLong(value);
	}

	/**
	 * @see java.nio.ByteBuffer#putShort(int, short)
	 */
	public ByteBuffer putShort(int index, short value) {
		return backing.putShort(index, value);
	}

	/**
	 * @see java.nio.ByteBuffer#putShort(short)
	 */
	public ByteBuffer putShort(short value) {
		return backing.putShort(value);
	}

	/**
	 * @see java.nio.ByteBuffer#slice()
	 */
	public ByteBuffer slice() {
		return backing.slice();
	}

}
