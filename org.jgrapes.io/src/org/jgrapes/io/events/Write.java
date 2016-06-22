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

import java.nio.Buffer;

import org.jgrapes.io.DataConnection;

/**
 * This event signals that a new chunk of data is to be forwarded to the
 * destination. The data is kept in a buffer.
 * 
 * @author Michael N. Lipp
 */
public class Write<T extends Buffer> 
	extends ConnectionEvent<Void, DataConnection<T>> {

	private T buffer;
	private int lockCount = 0;
	private boolean handled = false;
	
	/**
	 * Create a new event with the given buffer that must have been
	 * obtained from the connection's 
	 * {@link DataConnection#acquireWriteBuffer()}. Creating the event
	 * flips the buffer as it is assumed to be used for reading by
	 * the handlers(s) from now on.
	 * 
	 * @param the connection to write the data to
	 * @param buffer the buffer with the data
	 */
	public Write(DataConnection<T> connection, T buffer) {
		super(connection);
		this.buffer = buffer;
		buffer.flip();
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
	 * Increases the buffer's lock count. If the buffer is needed
	 * for some asynchronously running operation after the event has
	 * completed, it must be locked in order to prevent it from being
	 * released prematurely.
	 * 
	 * @throws IllegalStateException if the buffer has been released already
	 */
	synchronized public void lockBuffer() throws IllegalStateException {
		if (buffer == null) {
			throw new IllegalStateException("Buffer released already.");
		}
		lockCount += 1;
	}

	/**
	 * Decreases the buffer's lock count. If the lock count reached
	 * zero and the event has been handled by all handlers, the buffer
	 * is released. 
	 * 
	 * @throws IllegalStateException if the buffer is not locked or 
	 * has been released already
	 */
	synchronized public void unlockBuffer() throws IllegalStateException {
		if (buffer == null || lockCount == 0) {
			throw new IllegalStateException
				("Buffer not locked or released already.");
		}
		lockCount -= 1;
		if (handled && lockCount == 0) {
			getConnection().releaseWriteBuffer(buffer);
			buffer = null;
		}
	}
	
	/**
	 * Releases the buffer, unless locked.
	 */
	@Override
	synchronized protected void done() {
		handled = true;
		if (lockCount == 0) {
			getConnection().releaseWriteBuffer(buffer);
			buffer = null;
		}
	}
	
}
