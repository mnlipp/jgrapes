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
package org.jgrapes.core.events;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jgrapes.core.Event;

/**
 * A utility class for implementing completed events.
 * Completed events should provide the event that has been completed
 * as attribute. This class handles this attribute and can be used as
 * a convenient base class.
 * 
 * @author Michael N. Lipp
 */
public abstract class AbstractCompletedEvent extends Event {
	private Event initialEvent;

	/**
	 * Convenience method that creates ans sets a completed event for the given
	 * event with the given type.
	 * 
	 * @param event the event
	 * @param clazz the type of the completed event
	 * @return the event passed in as parameter (for method chaining)
	 */
	public static Event setCompletedEvent
		(Event event, Class<? extends AbstractCompletedEvent> clazz) {
		try {
			for (Constructor<?> c: clazz.getConstructors()) {
				if (c.getParameterTypes().length != 1) {
					continue;
				}
				if (!Event.class.isAssignableFrom(c.getParameterTypes()[0])) {
					continue;
				}
				event.setCompletedEvent ((Event)c.newInstance(event));
				return event;
			}
			throw new IllegalArgumentException
				("Class " + clazz.getName() + " has no <init>(Event)");
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException | SecurityException e) {
			throw (RuntimeException)
				(new IllegalArgumentException()).initCause(e);
		}
	}
		
	/**
	 * Create a new event that is to be fired upon the completion of the
	 * given (initial) event.
	 * 
	 * @param initialEvent
	 */
	protected AbstractCompletedEvent(Event initialEvent) {
		super();
		this.initialEvent = initialEvent;
	}

	/**
	 * @return the initialEvent
	 */
	public Event getInitialEvent() {
		return initialEvent;
	}
}
