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

import org.jgrapes.core.Event;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * This event signals that a new chunk of data has successfully been obtained
 * from some source. The data is kept in a buffer. The buffer is returned to the
 * pool upon successful processing of the event. This type of event is commonly
 * used for data flowing into the application.
 * 
 * @author Michael N. Lipp
 */
public class Input<T extends ManagedBuffer<?>> extends Event<Void> {

	private T buffer;
	
	/**
	 * Create a new event with the given buffer.
	 * 
	 * @param buffer the buffer with the data
	 */
	public Input(T buffer) {
		this.buffer = buffer;
	}

	/**
	 * Get the buffer with the data from this event.
	 * 
	 * @return the buffer
	 */
	public T getBuffer() {
		return buffer;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.EventBase#stopped()
	 */
	@Override
	protected void handled() {
		buffer.unlockBuffer();
	}
	
}
