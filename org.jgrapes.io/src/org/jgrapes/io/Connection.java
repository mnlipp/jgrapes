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
package org.jgrapes.io;

import java.nio.Buffer;

import org.jgrapes.core.Channel;

/**
 * Represents an I/O connection.
 * 
 * @author Michael N. Lipp
 */
public interface Connection<T extends Buffer> {

	/**
	 * A channel that can be used to send connection related events to.
	 * 
	 * @return the channel
	 */
	Channel getChannel();

	/**
	 * Get a buffer suitable to be passed to {@link Write} events.
	 * 
	 * @return the buffer
	 * @throws InterruptedException if the invoking thread is interrupted
	 * while waiting for a buffer
	 */
	T acquireWriteBuffer() throws InterruptedException;

	/**
	 * Releases a buffer used by a {@link Read} event. This method is invoked
	 * automatically upon the completion of a {@link Read} event.
	 * 
	 * @param buffer
	 */
	void releaseReadBuffer(T buffer);
}
