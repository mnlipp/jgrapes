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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A queue based buffer pool.
 * 
 * @author Michael N. Lipp
 */
public class ManagedBufferQueue<W extends ManagedBuffer<T>, T extends Buffer>
	implements BufferCollector {

	private Constructor<W> wrapper = null;
	private BlockingQueue<W> queue;
	
	/**
	 * Create a pool that contains the (wrapped) buffers as initial content.
	 * 
	 * @param wrapped the class to wrap the buffers in
	 * @param buffers the buffers to wrap
	 */
	@SafeVarargs
	public ManagedBufferQueue(Class<W> wrapped, T... buffers) {
		try {
			@SuppressWarnings("unchecked")
			Constructor<W>[] constructors 
				= (Constructor<W>[])wrapped.getConstructors();
			for (Constructor<W> c: constructors) {
				Class<?>[] paramTypes = c.getParameterTypes();
				if(paramTypes.length == 2
						&& Buffer.class.isAssignableFrom(paramTypes[0])
						&& paramTypes[1].equals(BufferCollector.class)) {
					wrapper = c;
					break;
				}
			}
			if (wrapper == null) {
				throw new IllegalArgumentException
					(wrapped + " is not a valid wrapper class.");
			}
		} catch (SecurityException e) {
		}
		queue = new ArrayBlockingQueue<W>(buffers.length);
		for (T buffer: buffers) {
			addBuffer(buffer);
		}
	}

	/**
	 * Wraps the given buffer as {@link ManagedBuffer} and adds it to the
	 * pool. 
	 * 
	 * @param buffer the buffer to add
	 */
	public void addBuffer(T buffer) {
		try {
			queue.add(wrapper.newInstance(buffer, this));
		} catch (InstantiationException | IllegalAccessException
		        | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Acquires a managed buffer from the pool. If the pool is empty,
	 * waits for a buffer to become available. The buffer has a lock count
	 * of one.
	 * 
	 * @return the acquired buffer
	 * @throws InterruptedException if the current thread is interrupted
	 */
	public W acquire() throws InterruptedException {
		return queue.take();
	}
	
	/**
	 * Re-adds the buffer to the pool. The buffer is cleared.
	 * 
	 * @see org.jgrapes.io.util.BufferCollector#recollect(org.jgrapes.io.util.ManagedBuffer)
	 */
	@Override
	public void recollect(ManagedBuffer<?> buffer) {
		buffer.clear();
		buffer.lockBuffer();
		@SuppressWarnings("unchecked")
		W b = (W)buffer;
		queue.add(b);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ManagedBufferQueue [");
		if (queue != null) {
			builder.append("queue=");
			builder.append(queue);
		}
		builder.append("]");
		return builder.toString();
	}
	
	
}
