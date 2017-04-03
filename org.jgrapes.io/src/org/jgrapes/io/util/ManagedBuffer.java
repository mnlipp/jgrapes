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

import java.nio.Buffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper around a {@link Buffer} that maintains a lock count for that
 * buffer. All methods known from {@code Buffer} are provided and
 * delegate to the backing buffer. Managed buffers can be used to maintain
 * pools of buffers. Buffers are locked when retrieved from the pool
 * and can automatically be returned when the last lock is released. 
 */
public abstract class ManagedBuffer<T extends Buffer> {

	protected T backing;
	private BufferCollector manager;
	private AtomicInteger lockCount = new AtomicInteger(1);
	
	/**
	 * Create a new Managed buffer, backed by the given buffer,
	 * with a lock count of one.
	 * 
	 * @param buffer the backing buffer
	 * @param manager used for restoring the buffer when the lock 
	 * count reaches zero
	 */
	public ManagedBuffer(T buffer, BufferCollector manager) {
		this.backing = buffer;
		this.manager = manager;
	}

	/**
	 * Return the backing buffer.
	 * 
	 * @return the buffer
	 */
	public T backingBuffer() {
		return backing;
	}

	/**
	 * Increases the buffer's lock count.
	 * 
	 * @return the managed buffer for easy chaining
	 */
	public ManagedBuffer<T> lockBuffer() {
		lockCount.incrementAndGet();
		return this;
	}

	/**
	 * Decreases the buffer's lock count. If the lock count reached
	 * zero, the buffer collect's {@link BufferCollector#recollect}
	 * method is invoked. 
	 * 
	 * @throws IllegalStateException if the buffer is not locked or 
	 * has been released already
	 */
	public void unlockBuffer() throws IllegalStateException {
		int locks = lockCount.decrementAndGet();
		if (locks < 0) {
			throw new IllegalStateException(
					"Buffer not locked or released already.");
		}
		if (locks == 0) {
			manager.recollect(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		builder.append(" [");
		if (backing != null) {
			builder.append("buffer=");
			builder.append(backing);
			builder.append(", ");
		}
		if (lockCount != null) {
			builder.append("lockCount=");
			builder.append(lockCount);
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @return the backing array
	 * @see java.nio.Buffer#array()
	 */
	public Object array() {
		return backing.array();
	}

	/**
	 * @return the backing array offset
	 * @see java.nio.Buffer#arrayOffset()
	 */
	public int arrayOffset() {
		return backing.arrayOffset();
	}

	/**
	 * @return the capacity
	 * @see java.nio.Buffer#capacity()
	 */
	public final int capacity() {
		return backing.capacity();
	}

	/**
	 * @return the buffer
	 * @see java.nio.Buffer#clear()
	 */
	public final Buffer clear() {
		return backing.clear();
	}

	/**
	 * @return the result
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof ManagedBuffer)) {
			return false;
		}
		return backing.equals(((ManagedBuffer<?>)obj).backingBuffer());
	}

	/**
	 * @return the buffer
	 * @see java.nio.Buffer#flip()
	 */
	public final Buffer flip() {
		return backing.flip();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#hasArray()
	 */
	public boolean hasArray() {
		return backing.hasArray();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#hasRemaining()
	 */
	public final boolean hasRemaining() {
		return backing.hasRemaining();
	}

	/**
	 * @return the result
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return backing.hashCode();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#isDirect()
	 */
	public boolean isDirect() {
		return backing.isDirect();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return backing.isReadOnly();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#limit()
	 */
	public final int limit() {
		return backing.limit();
	}

	/**
	 * @param newLimit the new limit
	 * @return the result
	 * @see java.nio.Buffer#limit(int)
	 */
	public final Buffer limit(int newLimit) {
		return backing.limit(newLimit);
	}

	/**
	 * @return the buffer
	 * @see java.nio.Buffer#mark()
	 */
	public final Buffer mark() {
		return backing.mark();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#position()
	 */
	public final int position() {
		return backing.position();
	}

	/**
	 * @param newPosition the new position
	 * @return the buffer
	 * @see java.nio.Buffer#position(int)
	 */
	public final Buffer position(int newPosition) {
		return backing.position(newPosition);
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#remaining()
	 */
	public final int remaining() {
		return backing.remaining();
	}

	/**
	 * @return the Buffer
	 * @see java.nio.Buffer#reset()
	 */
	public final Buffer reset() {
		return backing.reset();
	}

	/**
	 * @return the Buffer
	 * @see java.nio.Buffer#rewind()
	 */
	public final Buffer rewind() {
		return backing.rewind();
	}

}
