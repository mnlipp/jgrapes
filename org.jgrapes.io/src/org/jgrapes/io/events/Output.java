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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.io.util.ManagedCharBuffer;

/**
 * This event signals that a new chunk of internally generated data is to be
 * forwarded to some destination. The data is kept in a buffer. The buffer is
 * returned to the pool upon successful processing of the event. This type of
 * event is commonly used for data flowing out of the application.
 */
public class Output<T extends ManagedBuffer<?>>	extends Event<Void> {

	private T buffer;
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
	private Output(T buffer, boolean flip, boolean endOfRecord) {
		this.buffer = buffer;
		this.eor = endOfRecord;
		if (flip) {
			buffer.flip();
		}
	}
	
	/**
	 * Create a new event with the given buffer. Creating the event
	 * flips the buffer as it is assumed to be used for reading by
	 * the handlers(s) from now on.
	 * 
	 * @param buffer the buffer with the data
	 * @param endOfRecord if the event ends a data record
	 */
	public Output(T buffer, boolean endOfRecord) {
		this(buffer, true, endOfRecord);
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
	 * Convenience method that wraps a String in a 
	 * {@code Write<ManagedCharBuffer} event.
	 * 
	 * @param data the string to wrap
	 * @param endOfRecord if the event ends a data record
	 * @return the event
	 */
	public static Output<ManagedCharBuffer> 
		wrap(String data, boolean endOfRecord) {
		return new Output<>(new ManagedCharBuffer(data), false, endOfRecord);
	}
	
	/**
	 * Convenience method that wraps a byte array in a 
	 * {@code Write<ManagedByteBuffer} event.
	 * 
	 * @param data the array to wrap
	 * @param endOfRecord if the event ends a data record
	 * @return the event
	 */
	public static Output<ManagedByteBuffer> 
		wrap(byte[] data, boolean endOfRecord) {
		return new Output<>(new ManagedByteBuffer(data), false, endOfRecord);
	}
	
	/**
	 * Get the buffer with the data from this event.
	 * 
	 * @return the buffer
	 */
	public T buffer() {
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
}
