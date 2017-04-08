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

package org.jgrapes.core;

import org.jgrapes.core.Components.IdInfoProvider;

/**
 * An interface that describes a queue of events that are sent to the components
 * of the associated tree. Any events fired by the components while handling
 * an event from the pipeline are added at the end of the pipeline.
 * <P>
 * An event pipeline is run by a single thread from a thread pool. Adding
 * several events to the same pipeline therefore ensures that they are executed
 * in sequence.
 */
public interface EventPipeline extends IdInfoProvider {

	/**
	 * Add an event to be sent to components listening for such events on
	 * the given channels to the end of the queue. If no channels are
	 * specified as parameters, the event is fired on the event's 
	 * channel (see {@link Event#channels()}). If the event doesn't
	 * specify channels either, the channel depends on how the
	 * event pipeline was obtained. Event pipelines obtained from a
	 * component's manager use the component's channel as fall back.
	 * 
	 * @param <T> the event's type
	 * @param event the event to process
	 * @param channels the channels that the event was fired on
	 * @return the event (for easy chaining)
	 */
	<T extends Event<?>> T fire(T event, Channel... channels);

	/**
	 * All pipelines use the same id scope to make them uniquely identifiable
	 * by their number.
	 */
	@Override
	default Class<?> idScope() {
		return EventPipeline.class;
	}	
}
