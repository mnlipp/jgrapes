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
package org.jgrapes.io.events;

import org.jgrapes.io.Connection;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.io.util.ManagedCharBuffer;

/**
 * This event signals that a new chunk of internally generated data is to be
 * forwarded to some destination. The data is kept in a buffer. The buffer is
 * returned to the pool upon successful processing of the event. This type of
 * event is commonly used for data flowing out of the application.
 * 
 * @author Michael N. Lipp
 */
public class Output<T extends ManagedBuffer<?>> 
	extends ConnectionEvent<Void> {

	private T buffer;

	/**
	 * Create a new write event with the given buffer and optionally flips
	 * it. Used internally for constructor ("super(...)") invocations that 
	 * don't flip the buffer.
	 * 
	 * @param connection the connection to write the data to
	 * @param buffer the buffer with the data
	 * @param flip
	 */
	private Output(Connection connection, T buffer, boolean flip) {
		super(connection);
		this.buffer = buffer;
		if (flip) {
			buffer.flip();
		}
	}
	
	/**
	 * Create a new event with the given buffer. Creating the event
	 * flips the buffer as it is assumed to be used for reading by
	 * the handlers(s) from now on.
	 * 
	 * @param connection the connection to write the data to
	 * @param buffer the buffer with the data
	 */
	public Output(Connection connection, T buffer) {
		this(connection, buffer, true);
	}

	/**
	 * Convenience method that wraps a String in a 
	 * {@code Write<ManagedCharBuffer} event.
	 * 
	 * @param connection the connection to write the data to
	 * @param data the string to wrap
	 * @return the event
	 */
	public static Output<ManagedCharBuffer> 
			wrap(Connection connection, String data) {
		return new Output<>(connection, new ManagedCharBuffer(data), false);
	}
	
	/**
	 * Convenience method that wraps a byte array in a 
	 * {@code Write<ManagedByteBuffer} event.
	 * 
	 * @param connection the connection to write the data to
	 * @param data the array to wrap
	 * @return the event
	 */
	public static Output<ManagedByteBuffer> 
			wrap(Connection connection, byte[] data) {
		return new Output<>(connection, new ManagedByteBuffer(data), false);
	}
	
	/**
	 * Get the buffer with the data from this event.
	 * 
	 * @return the buffer
	 */
	public T getBuffer() {
		return buffer;
	}

	/**
	 * Releases the buffer, unless locked.
	 */
	@Override
	synchronized protected void handled() {
		buffer.unlockBuffer();
	}
	
}
