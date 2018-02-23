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
import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * Events of this type signal that a new chunk of data is available
 * for processing. From a consumer's point of view, the data is
 * kept in a NIO {@link Buffer}. However, when creating the event
 * the data has to be provided as a {@link ManagedBuffer}. 
 * This buffer is returned to the pool upon successful processing 
 * of the event.
 * 
 * As a convenience, the class provides the methods known
 * from {@link Buffer} as short-cuts for invoking
 * `data().`*method()*.
 * 
 * @param <T> the type of data used in this event
 */
public abstract class IOEvent<T extends Buffer> extends Event<Void> {

	private ManagedBuffer<T> buffer;
	private boolean eor;
	
	protected IOEvent(ManagedBuffer<T> buffer, boolean endOfRecord) {
		this.buffer = buffer;
		this.eor = endOfRecord;
	}

	/**
	 * Get the managed buffer with the data from this event.
	 * 
	 * @return the buffer
	 */
	public ManagedBuffer<T> buffer() {
		return buffer;
	}

	/**
	 * Return the data associated with this event 
	 * as {@link Buffer}. This is short for
	 * `buffer().backingBuffer()`.
	 *
	 * @return the data
	 */
	public T data() {
		return buffer.backingBuffer();
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
	
	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.EventBase#stopped()
	 */
	@Override
	protected void handled() {
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
		builder.append(",size=");
		Optional.ofNullable(buffer).map(
				b -> b.backingBuffer().position()).orElse(0);
		builder.append(",eor=");
		builder.append(eor);
		builder.append("]");
		return builder.toString();
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
	 * @see java.nio.Buffer#remaining()
	 */
	public final int remaining() {
		return buffer.remaining();
	}
}
