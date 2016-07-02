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
 * @author Michael N. Lipp
 *
 */
public class ManagedByteBuffer extends ManagedBuffer<ByteBuffer> {

	public ManagedByteBuffer(ByteBuffer buffer, BufferCollector manager) {
		super(buffer, manager);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#array()
	 */
	@Override
	public final byte[] array() {
		return buffer.array();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asCharBuffer()
	 */
	public CharBuffer asCharBuffer() {
		return buffer.asCharBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asDoubleBuffer()
	 */
	public DoubleBuffer asDoubleBuffer() {
		return buffer.asDoubleBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asFloatBuffer()
	 */
	public FloatBuffer asFloatBuffer() {
		return buffer.asFloatBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asIntBuffer()
	 */
	public IntBuffer asIntBuffer() {
		return buffer.asIntBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asLongBuffer()
	 */
	public LongBuffer asLongBuffer() {
		return buffer.asLongBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asReadOnlyBuffer()
	 */
	public ByteBuffer asReadOnlyBuffer() {
		return buffer.asReadOnlyBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#asShortBuffer()
	 */
	public ShortBuffer asShortBuffer() {
		return buffer.asShortBuffer();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#compact()
	 */
	public ByteBuffer compact() {
		return buffer.compact();
	}

	/**
	 * @param that
	 * @return
	 * @see java.nio.ByteBuffer#compareTo(java.nio.ByteBuffer)
	 */
	public int compareTo(ByteBuffer that) {
		return buffer.compareTo(that);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#duplicate()
	 */
	public ByteBuffer duplicate() {
		return buffer.duplicate();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#get()
	 */
	public byte get() {
		return buffer.get();
	}

	/**
	 * @param dst
	 * @param offset
	 * @param length
	 * @return
	 * @see java.nio.ByteBuffer#get(byte[], int, int)
	 */
	public ByteBuffer get(byte[] dst, int offset, int length) {
		return buffer.get(dst, offset, length);
	}

	/**
	 * @param dst
	 * @return
	 * @see java.nio.ByteBuffer#get(byte[])
	 */
	public ByteBuffer get(byte[] dst) {
		return buffer.get(dst);
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#get(int)
	 */
	public byte get(int index) {
		return buffer.get(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#getChar()
	 */
	public char getChar() {
		return buffer.getChar();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getChar(int)
	 */
	public char getChar(int index) {
		return buffer.getChar(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#getDouble()
	 */
	public double getDouble() {
		return buffer.getDouble();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getDouble(int)
	 */
	public double getDouble(int index) {
		return buffer.getDouble(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#getFloat()
	 */
	public float getFloat() {
		return buffer.getFloat();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getFloat(int)
	 */
	public float getFloat(int index) {
		return buffer.getFloat(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#getInt()
	 */
	public int getInt() {
		return buffer.getInt();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getInt(int)
	 */
	public int getInt(int index) {
		return buffer.getInt(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#getLong()
	 */
	public long getLong() {
		return buffer.getLong();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getLong(int)
	 */
	public long getLong(int index) {
		return buffer.getLong(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#getShort()
	 */
	public short getShort() {
		return buffer.getShort();
	}

	/**
	 * @param index
	 * @return
	 * @see java.nio.ByteBuffer#getShort(int)
	 */
	public short getShort(int index) {
		return buffer.getShort(index);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#order()
	 */
	public final ByteOrder order() {
		return buffer.order();
	}

	/**
	 * @param bo
	 * @return
	 * @see java.nio.ByteBuffer#order(java.nio.ByteOrder)
	 */
	public final ByteBuffer order(ByteOrder bo) {
		return buffer.order(bo);
	}

	/**
	 * @param b
	 * @return
	 * @see java.nio.ByteBuffer#put(byte)
	 */
	public ByteBuffer put(byte b) {
		return buffer.put(b);
	}

	/**
	 * @param src
	 * @param offset
	 * @param length
	 * @return
	 * @see java.nio.ByteBuffer#put(byte[], int, int)
	 */
	public ByteBuffer put(byte[] src, int offset, int length) {
		return buffer.put(src, offset, length);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.ByteBuffer#put(byte[])
	 */
	public final ByteBuffer put(byte[] src) {
		return buffer.put(src);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ByteBuffer src) {
		return buffer.put(src);
	}

	/**
	 * @param src
	 * @return
	 * @see java.nio.ByteBuffer#put(java.nio.ByteBuffer)
	 */
	public ByteBuffer put(ManagedByteBuffer src) {
		return buffer.put(src.getBuffer());
	}

	/**
	 * @param index
	 * @param b
	 * @return
	 * @see java.nio.ByteBuffer#put(int, byte)
	 */
	public ByteBuffer put(int index, byte b) {
		return buffer.put(index, b);
	}

	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putChar(char)
	 */
	public ByteBuffer putChar(char value) {
		return buffer.putChar(value);
	}

	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putChar(int, char)
	 */
	public ByteBuffer putChar(int index, char value) {
		return buffer.putChar(index, value);
	}

	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putDouble(double)
	 */
	public ByteBuffer putDouble(double value) {
		return buffer.putDouble(value);
	}

	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putDouble(int, double)
	 */
	public ByteBuffer putDouble(int index, double value) {
		return buffer.putDouble(index, value);
	}

	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putFloat(float)
	 */
	public ByteBuffer putFloat(float value) {
		return buffer.putFloat(value);
	}

	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putFloat(int, float)
	 */
	public ByteBuffer putFloat(int index, float value) {
		return buffer.putFloat(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putInt(int, int)
	 */
	public ByteBuffer putInt(int index, int value) {
		return buffer.putInt(index, value);
	}

	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putInt(int)
	 */
	public ByteBuffer putInt(int value) {
		return buffer.putInt(value);
	}

	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putLong(int, long)
	 */
	public ByteBuffer putLong(int index, long value) {
		return buffer.putLong(index, value);
	}

	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putLong(long)
	 */
	public ByteBuffer putLong(long value) {
		return buffer.putLong(value);
	}

	/**
	 * @param index
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putShort(int, short)
	 */
	public ByteBuffer putShort(int index, short value) {
		return buffer.putShort(index, value);
	}

	/**
	 * @param value
	 * @return
	 * @see java.nio.ByteBuffer#putShort(short)
	 */
	public ByteBuffer putShort(short value) {
		return buffer.putShort(value);
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#slice()
	 */
	public ByteBuffer slice() {
		return buffer.slice();
	}

	/**
	 * @return
	 * @see java.nio.ByteBuffer#toString()
	 */
	public String toString() {
		return buffer.toString();
	}
	
}
