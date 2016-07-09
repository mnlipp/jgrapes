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
 * delegate to the backing buffer.
 * 
 * @author Michael N. Lipp
 */
public abstract class ManagedBuffer<T extends Buffer> {

	/**
	 * A buffer collector that does nothiong when the managed buffer
	 * is no longer used.
	 */
	public static final BufferCollector NOOP_COLLECTOR 
		= new BufferCollector() {
			@Override
			public void recollect(ManagedBuffer<?> buffer) {
			}
		};

	protected T buffer;
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
		this.buffer = buffer;
		this.manager = manager;
	}

	/**
	 * Return the backing buffer.
	 * 
	 * @return the buffer
	 */
	public T getBuffer() {
		return buffer;
	}

	/**
	 * Increases the buffer's lock count.
	 */
	public void lockBuffer() {
		lockCount.incrementAndGet();
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
			throw new IllegalStateException
				("Buffer not locked or released already.");
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
		if (buffer != null) {
			builder.append("buffer=");
			builder.append(buffer);
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
	 * @see java.nio.Buffer#array()
	 */
	public Object array() {
		return buffer.array();
	}

	/**
	 * @see java.nio.Buffer#arrayOffset()
	 */
	public int arrayOffset() {
		return buffer.arrayOffset();
	}

	/**
	 * @see java.nio.Buffer#capacity()
	 */
	public final int capacity() {
		return buffer.capacity();
	}

	/**
	 * @see java.nio.Buffer#clear()
	 */
	public final Buffer clear() {
		return buffer.clear();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof ManagedBuffer)) {
			return false;
		}
		return buffer.equals(((ManagedBuffer<?>)obj).getBuffer());
	}

	/**
	 * @see java.nio.Buffer#flip()
	 */
	public final Buffer flip() {
		return buffer.flip();
	}

	/**
	 * @see java.nio.Buffer#hasArray()
	 */
	public boolean hasArray() {
		return buffer.hasArray();
	}

	/**
	 * @see java.nio.Buffer#hasRemaining()
	 */
	public final boolean hasRemaining() {
		return buffer.hasRemaining();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return buffer.hashCode();
	}

	/**
	 * @see java.nio.Buffer#isDirect()
	 */
	public boolean isDirect() {
		return buffer.isDirect();
	}

	/**
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return buffer.isReadOnly();
	}

	/**
	 * @see java.nio.Buffer#limit()
	 */
	public final int limit() {
		return buffer.limit();
	}

	/**
	 * @see java.nio.Buffer#limit(int)
	 */
	public final Buffer limit(int newLimit) {
		return buffer.limit(newLimit);
	}

	/**
	 * @see java.nio.Buffer#mark()
	 */
	public final Buffer mark() {
		return buffer.mark();
	}

	/**
	 * @see java.nio.Buffer#position()
	 */
	public final int position() {
		return buffer.position();
	}

	/**
	 * @see java.nio.Buffer#position(int)
	 */
	public final Buffer position(int newPosition) {
		return buffer.position(newPosition);
	}

	/**
	 * @see java.nio.Buffer#remaining()
	 */
	public final int remaining() {
		return buffer.remaining();
	}

	/**
	 * @see java.nio.Buffer#reset()
	 */
	public final Buffer reset() {
		return buffer.reset();
	}

	/**
	 * @see java.nio.Buffer#rewind()
	 */
	public final Buffer rewind() {
		return buffer.rewind();
	}

}
