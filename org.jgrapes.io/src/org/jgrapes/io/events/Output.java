/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io.events;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.io.util.BufferCollector;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * This event signals that a new chunk of internally generated data is to be
 * forwarded to some destination. The data is kept in a buffer. The buffer is
 * returned to the pool upon successful processing of the event. This type of
 * event is commonly used for data flowing out of the application.
 * 
 * As a convenience, the class provides the methods known
 * from {@link Buffer} as short-cuts for invoking
 * `buffer().`*method()*.
 */
public class Output<T extends Buffer> extends Event<Void> {

	private ManagedBuffer<T> buffer;
	private boolean eor;

	/**
	 * Create a new output event with the given buffer and optionally flips
	 * it. Used internally for constructor ("super(...)") invocations that 
	 * don't flip the buffer.
	 * 
	 * @param buffer the buffer with the data
	 * @param flip if the buffer should be flipped
	 * @param endOfRecord if the event ends a data record
	 */
	private Output(ManagedBuffer<T> buffer, boolean flip, boolean endOfRecord) {
		this.buffer = buffer;
		this.eor = endOfRecord;
		if (flip) {
			buffer.flip();
		}
	}
	
	/**
	 * Create a new event from an existing event. This constructor
	 * is useful if the data is to be forwarded to another channel
	 * by a new event.
	 * 
	 * The buffer is reused in the new event (the lock count is 
	 * incremented).
	 * 
	 * @param event the existing event
	 */
	public Output(Output<T> event) {
		this(event.buffer(), false, event.isEndOfRecord());
		event.buffer().lockBuffer();
	}
	
	/**
	 * Create a new event with the given buffer. The buffer must
	 * have been prepared for invoking `get`-methods.
	 * 
	 * @param buffer the buffer with the data
	 * @param endOfRecord if the event ends a data record
	 */
	public static <B extends Buffer> Output<B> fromSource(
			ManagedBuffer<B> buffer, boolean endOfRecord) {
		return new Output<>(buffer, false, endOfRecord);
	}

	/**
	 * Create a new event with the given buffer. Creating the event
	 * flips the buffer, which is assumed to have been used for
	 * collecting data up to now.
	 * 
	 * @param buffer the buffer with the data
	 * @param endOfRecord if the event ends a data record
	 */
	public static <B extends Buffer> Output<B> fromSink(
			ManagedBuffer<B> buffer, boolean endOfRecord) {
		return new Output<>(buffer, true, endOfRecord);
	}

	/**
	 * Convenience method that creates a 
	 * {@code Output<CharBuffer} event from a {@link String}.
	 * 
	 * @param data the string to wrap
	 * @param endOfRecord if the event ends a data record
	 * @return the event
	 */
	public static Output<CharBuffer> 
		from(String data, boolean endOfRecord) {
		return new Output<>(new ManagedBuffer<>(
				CharBuffer.wrap(data), BufferCollector.NOOP_COLLECTOR),
				false, endOfRecord);
	}
	
	/**
	 * Convenience method that creates a 
	 * {@code Output<ByteBuffer} event from a `byte[]`.
	 * 
	 * @param data the array to wrap
	 * @param endOfRecord if the event ends a data record
	 * @return the event
	 */
	public static Output<ByteBuffer> 
		from(byte[] data, boolean endOfRecord) {
		return new Output<>(new ManagedBuffer<ByteBuffer>(
				ByteBuffer.wrap(data), BufferCollector.NOOP_COLLECTOR),
				false, endOfRecord);
	}
	
	/**
	 * Get the buffer with the data from this event.
	 * 
	 * @return the buffer
	 */
	public ManagedBuffer<T> buffer() {
		return buffer;
	}

	/**
	 * Return the end of record flag passed to the constructor.
	 * The precise interpretation of a record depends on the data
	 * handled. 
	 * 
	 * @return the end of record flag
	 */
	public boolean isEndOfRecord() {
		return eor;
	}
	
	/**
	 * Releases the buffer, unless locked.
	 */
	@Override
	protected synchronized void handled() {
		buffer.unlockBuffer();
		buffer = null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append(",eor=");
		builder.append(eor);
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Return the backing buffer of the managed buffer.
	 * Short for `buffer().backingBuffer()`.
	 *
	 * @return the buffer
	 */
	public Buffer backingBuffer() {
		return buffer.backingBuffer();
	}
	
	/**
	 * @return the backing array
	 * @see java.nio.Buffer#array()
	 */
	public Object array() {
		return buffer.array();
	}

	/**
	 * @return the backing array offset
	 * @see java.nio.Buffer#arrayOffset()
	 */
	public int arrayOffset() {
		return buffer.arrayOffset();
	}

	/**
	 * @return the capacity
	 * @see java.nio.Buffer#capacity()
	 */
	public final int capacity() {
		return buffer.capacity();
	}

	/**
	 * @return the buffer
	 * @see java.nio.Buffer#clear()
	 */
	public final Buffer clear() {
		return buffer.clear();
	}

	/**
	 * @return the buffer
	 * @see java.nio.Buffer#flip()
	 */
	public final Buffer flip() {
		return buffer.flip();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#hasArray()
	 */
	public boolean hasArray() {
		return buffer.hasArray();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#hasRemaining()
	 */
	public final boolean hasRemaining() {
		return buffer.hasRemaining();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#isDirect()
	 */
	public boolean isDirect() {
		return buffer.isDirect();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#isReadOnly()
	 */
	public boolean isReadOnly() {
		return buffer.isReadOnly();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#limit()
	 */
	public final int limit() {
		return buffer.limit();
	}

	/**
	 * @param newLimit the new limit
	 * @return the result
	 * @see java.nio.Buffer#limit(int)
	 */
	public final Buffer limit(int newLimit) {
		return buffer.limit(newLimit);
	}

	/**
	 * @return the buffer
	 * @see java.nio.Buffer#mark()
	 */
	public final Buffer mark() {
		return buffer.mark();
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#position()
	 */
	public final int position() {
		return buffer.position();
	}

	/**
	 * @param newPosition the new position
	 * @return the buffer
	 * @see java.nio.Buffer#position(int)
	 */
	public final Buffer position(int newPosition) {
		return buffer.position(newPosition);
	}

	/**
	 * @return the result
	 * @see java.nio.Buffer#remaining()
	 */
	public final int remaining() {
		return buffer.remaining();
	}

	/**
	 * @return the Buffer
	 * @see java.nio.Buffer#reset()
	 */
	public final Buffer reset() {
		return buffer.reset();
	}

	/**
	 * @return the Buffer
	 * @see java.nio.Buffer#rewind()
	 */
	public final Buffer rewind() {
		return buffer.rewind();
	}
}
