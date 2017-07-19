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
import java.util.function.Supplier;

/**
 * A queue based buffer pool.
 */
public class ManagedBufferQueue<W extends ManagedBuffer<T>, T extends Buffer>
	implements BufferCollector {

	private BiFunction<T, BufferCollector,W> wrapper = null;
	private Supplier<T> bufferFactory = null;
	private BlockingQueue<W> queue;
	private int bufferSize;
	private int minBufs;
	private int maxBufs;
	private int createdBufs;
	
	/**
	 * Create a pool that contains a varying number of (wrapped) buffers.
	 * The minimum number of buffers specified will be pre-allocated
	 * upon the creation of the queue. When acquiring buffers, those will be
	 * handed out first. If no buffers are left in the queue, additional 
	 * buffers are created up to the given maximum for the total number of
	 * buffers. Recollected buffers are put in the queue until it holds
	 * the specified minimum. Any additional recollected buffers are
	 * discarded. 
	 * 
	 * @param wrapper the function that converts buffers to managed buffers
	 * @param bufferFactory a function that creates a new buffer
	 * @param buffersMin the minimum number of buffers
	 * @param buffersMax the maximum number of buffers
	 */
	public ManagedBufferQueue(BiFunction<T,BufferCollector, W> wrapper, 
			Supplier<T> bufferFactory, int buffersMin, int buffersMax) {
		this.wrapper = wrapper;
		this.bufferFactory = bufferFactory;
		minBufs = buffersMin;
		maxBufs = buffersMax;
		createdBufs = 0;
		queue = new ArrayBlockingQueue<W>(Math.max(1, buffersMin));
		for (int i = 0; i < Math.max(1, buffersMin); i++) {
			queue.add(createBuffer());
		}
		bufferSize = queue.peek().capacity();
	}

	/**
	 * Create a pool that contains the given number of (wrapped) buffers.
	 * 
	 * @param wrapper the function that converts buffers to managed buffers
	 * @param bufferFactory a function that creates a new buffer
	 * @param buffers the number of buffers
	 */
	public ManagedBufferQueue(BiFunction<T,BufferCollector, W> wrapper, 
			Supplier<T> bufferFactory, int buffers) {
		this(wrapper, bufferFactory, buffers, buffers);
	}

	private W createBuffer() {
		createdBufs += 1;
		return wrapper.apply(this.bufferFactory.get(), this);
	}

	/**
	 * Returns the size of the buffers managed by this queue.
	 * 
	 * @return the buffer size
	 */
	public int bufferSize() {
		return bufferSize;
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
		if (queue.size() == 0 && createdBufs < maxBufs) {
			return createBuffer();
		}
		return queue.take();
	}
	
	/**
	 * Re-adds the buffer to the pool. The buffer is cleared.
	 * 
	 * @see org.jgrapes.io.util.BufferCollector#recollect(org.jgrapes.io.util.ManagedBuffer)
	 */
	@Override
	public void recollect(ManagedBuffer<?> buffer) {
		if (queue.size() < minBufs) {
			// Enqueue
			buffer.clear();
			buffer.lockBuffer();
			@SuppressWarnings("unchecked")
			W buf = (W)buffer;
			queue.add(buf);
		} else {
			// Discard
			createdBufs -= 1;
		}
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
