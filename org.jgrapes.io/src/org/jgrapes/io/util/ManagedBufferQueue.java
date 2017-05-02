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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

/**
 * A queue based buffer pool.
 */
public class ManagedBufferQueue<W extends ManagedBuffer<T>, T extends Buffer>
	implements BufferCollector {

	private BiFunction<T, BufferCollector,W> wrapper = null;
	private BlockingQueue<W> queue;
	
	/**
	 * Create a pool that contains the (wrapped) buffers as initial content.
	 * 
	 * @param wrapper the function that converts buffers to managed buffers
	 * @param buffers the buffers to wrap
	 */
	@SafeVarargs
	public ManagedBufferQueue(
			BiFunction<T,BufferCollector, W> wrapper, T... buffers) {
		this.wrapper = wrapper;
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
		queue.add(wrapper.apply(buffer, this));
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
		W buf = (W)buffer;
		queue.add(buf);
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
