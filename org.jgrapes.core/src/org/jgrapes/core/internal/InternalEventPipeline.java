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

package org.jgrapes.core.internal;

import java.util.concurrent.ExecutorService;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components.IdInfoProvider;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * Implemented by event pipelines. Note that contrary to the 
 * {@link EventPipeline} from the API, this interface does not define any
 * checking in its {@link #add(Event, Channel...)} method. Therefore
 * an {@code InternalEventPipeline} is not automatically an
 * {@code EventPipeline} (but it's a bit more efficient with regards to the
 * {@code add} method). 
 */
interface InternalEventPipeline extends IdInfoProvider {

	/**
	 * Add an event to be sent to components listening for such events on
	 * the given channels to the end of the queue without any checking.
	 * 
	 * @param <T> the event's type
	 * @param event the event to process
	 * @param channels the channels that the event was fired on
	 * @return the event (for easy chaining)
	 */
	<T extends Event<?>> T add(T event, Channel... channels);

	/**
	 * Restrict the source for events to the given pipeline.
	 *
	 * @param sourcePipeline the source pipeline
	 * @return the event pipeline
	 */
	void restrictEventSource(InternalEventPipeline sourcePipeline);
	
	/**
	 * Merge the events from the other event pipeline into this one.
	 * 
	 * @param other the other event pipeline
	 */
	void merge(InternalEventPipeline other);

	/**
	 * All pipelines use the same id scope to make them uniquely identifiable
	 * by their number.
	 */
	@Override
	default Class<?> idScope() {
		return EventPipeline.class;
	}	
	
	/**
	 * Returns the executor service used by this event pipeline to
	 * obtain its thread.
	 * 
	 * @return the executor service
	 */
	ExecutorService executorService();
}
