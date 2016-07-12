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
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.stream.IntStream;

/**
 * A specialized {@code ManagedBuffer<CharBuffer>} that provides
 * the same methods as {@code CharBuffer} by delegating to the
 * backing buffer.
 *
 * @author Michael N. Lipp
 */
public class ManagedCharBuffer extends ManagedBuffer<CharBuffer> {

	/**
	 * Creates new managed buffer that is backed by the given buffer
	 * and uses the given manager to release the buffer if it is no longer
	 * locked.
	 * 
	 * @param buffer
	 * @param manager
	 */
	public ManagedCharBuffer(CharBuffer buffer, BufferCollector manager) {
		super(buffer, manager);
	}

	/**
	 * Creates a new managed buffer that is backed by the given
	 * char sequence. The manager for this instance does nothing
	 * when the buffer is released.
	 * 
	 * @param backing
	 */
	public ManagedCharBuffer(CharSequence backing) {
		super(CharBuffer.wrap(backing), BufferCollector.NOOP_COLLECTOR);
	}
	
	/**
	 * @see java.nio.CharBuffer#append(char)
	 */
	public CharBuffer append(char c) {
		return backing.append(c);
	}

	/**
	 * @see java.nio.CharBuffer#append(java.lang.CharSequence, int, int)
	 */
	public CharBuffer append(CharSequence csq, int start, int end) {
		return backing.append(csq, start, end);
	}

	/**
	 * @see java.nio.CharBuffer#append(java.lang.CharSequence)
	 */
	public CharBuffer append(CharSequence csq) {
		return backing.append(csq);
	}

	/**
	 * @see java.nio.CharBuffer#array()
	 */
	@Override
	public final char[] array() {
		return backing.array();
	}

	/**
	 * @see java.nio.CharBuffer#arrayOffset()
	 */
	public final int arrayOffset() {
		return backing.arrayOffset();
	}

	/**
	 * @see java.nio.CharBuffer#asReadOnlyBuffer()
	 */
	public CharBuffer asReadOnlyBuffer() {
		return backing.asReadOnlyBuffer();
	}

	/**
	 * @see java.nio.CharBuffer#charAt(int)
	 */
	public final char charAt(int index) {
		return backing.charAt(index);
	}

	/**
	 * @see java.nio.CharBuffer#chars()
	 */
	public IntStream chars() {
		return backing.chars();
	}

	/**
	 * @see java.nio.CharBuffer#compact()
	 */
	public CharBuffer compact() {
		return backing.compact();
	}

	/**
	 * @see java.nio.CharBuffer#compareTo(java.nio.CharBuffer)
	 */
	public int compareTo(CharBuffer that) {
		return backing.compareTo(that);
	}

	/**
	 * @see java.nio.CharBuffer#duplicate()
	 */
	public CharBuffer duplicate() {
		return backing.duplicate();
	}

	/**
	 * @see java.nio.CharBuffer#get()
	 */
	public char get() {
		return backing.get();
	}

	/**
	 * @see java.nio.CharBuffer#get(char[], int, int)
	 */
	public CharBuffer get(char[] dst, int offset, int length) {
		return backing.get(dst, offset, length);
	}

	/**
	 * @see java.nio.CharBuffer#get(char[])
	 */
	public CharBuffer get(char[] dst) {
		return backing.get(dst);
	}

	/**
	 * @see java.nio.CharBuffer#get(int)
	 */
	public char get(int index) {
		return backing.get(index);
	}

	/**
	 * @see java.nio.CharBuffer#hasArray()
	 */
	public final boolean hasArray() {
		return backing.hasArray();
	}

	/**
	 * @see java.nio.CharBuffer#isDirect()
	 */
	public boolean isDirect() {
		return backing.isDirect();
	}

	/**
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return backing.isReadOnly();
	}

	/**
	 * @see java.nio.CharBuffer#length()
	 */
	public final int length() {
		return backing.length();
	}

	/**
	 * @see java.nio.CharBuffer#order()
	 */
	public ByteOrder order() {
		return backing.order();
	}

	/**
	 * @see java.nio.CharBuffer#put(char)
	 */
	public CharBuffer put(char c) {
		return backing.put(c);
	}

	/**
	 * @see java.nio.CharBuffer#put(char[], int, int)
	 */
	public CharBuffer put(char[] src, int offset, int length) {
		return backing.put(src, offset, length);
	}

	/**
	 * @see java.nio.CharBuffer#put(char[])
	 */
	public final CharBuffer put(char[] src) {
		return backing.put(src);
	}

	/**
	 * @see java.nio.CharBuffer#put(java.nio.CharBuffer)
	 */
	public CharBuffer put(CharBuffer src) {
		return backing.put(src);
	}

	/**
	 * @see java.nio.CharBuffer#put(java.nio.CharBuffer)
	 */
	public CharBuffer put(ManagedCharBuffer src) {
		return backing.put(src.getBacking());
	}

	/**
	 * @see java.nio.CharBuffer#put(int, char)
	 */
	public CharBuffer put(int index, char c) {
		return backing.put(index, c);
	}

	/**
	 * @see java.nio.CharBuffer#put(java.lang.String, int, int)
	 */
	public CharBuffer put(String src, int start, int end) {
		return backing.put(src, start, end);
	}

	/**
	 * @see java.nio.CharBuffer#put(java.lang.String)
	 */
	public final CharBuffer put(String src) {
		return backing.put(src);
	}

	/**
	 * @see java.nio.CharBuffer#read(java.nio.CharBuffer)
	 */
	public int read(CharBuffer target) throws IOException {
		return backing.read(target);
	}

	/**
	 * @see java.nio.CharBuffer#slice()
	 */
	public CharBuffer slice() {
		return backing.slice();
	}

	/**
	 * @see java.nio.CharBuffer#subSequence(int, int)
	 */
	public CharBuffer subSequence(int start, int end) {
		return backing.subSequence(start, end);
	}

}
