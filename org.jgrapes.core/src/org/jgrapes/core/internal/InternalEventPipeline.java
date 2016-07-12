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
package org.jgrapes.core.internal;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * Implemented by event pipelines. Note that contrary to the 
 * {@link EventPipeline} from the API, this interface does not define any
 * checking in its {@link #add(Event, Channel...)} method. Therefore
 * an {@code InternalEventPipeline} is not automatically an
 * {@code EventPipeline} (but it's a bit more efficient with regards to the
 * {@code add} method). 
 * 
 * @author Michael N. Lipp
 */
interface InternalEventPipeline {

	/**
	 * Add an event to be sent to components listening for such events on
	 * the given channels to the end of the queue without any checking.
	 * 
	 * @param event the event to process
	 * @param channels the channels that the event was fired on
	 * @return the event (for easy chaining)
	 */
	<T extends Event<?>> T add(T event, Channel... channels);

	/**
	 * Merge the events from the other event pipeline into this one.
	 * 
	 * @param other the other event pipeline
	 */
	void merge (InternalEventPipeline other);	

}
