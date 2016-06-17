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
import org.jgrapes.core.internal.EventBase;

/**
 * A utility class for implementing completed events.
 * Completed events should provide the event that has been completed
 * as result.
 * 
 * @author Michael N. Lipp
 */
public abstract class AbstractCompletedEvent<T extends EventBase<?>>
		extends Event<T> {

	private T initialEvent;
	
	/**
	 * Convenience method that creates and sets a completed event for the given
	 * event with the given type.
	 * 
	 * @param event the event
	 * @param clazz the type of the completed event
	 * @return the event passed in as parameter (for method chaining)
	 */
	public static <T> Event<T> setCompletedEvent
		(Event<T> event, Class<? extends AbstractCompletedEvent
				<? extends EventBase<T>>> clazz) {
		try {
			@SuppressWarnings("unchecked")
			Constructor<? extends AbstractCompletedEvent
					<? extends EventBase<T>>>[] constrs
					= (Constructor<? extends AbstractCompletedEvent
							<? extends EventBase<T>>>[])clazz.getConstructors();
			for (Constructor<? extends AbstractCompletedEvent
					<? extends EventBase<T>>> c: constrs) {
				if (c.getParameterTypes().length != 1) {
					continue;
				}
				if (!Event.class.isAssignableFrom(c.getParameterTypes()[0])) {
					continue;
				}
				event.setCompletedEvent (c.newInstance(event));
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
	protected AbstractCompletedEvent(T initialEvent) {
		super();
		this.initialEvent = initialEvent;
		setResult(initialEvent);
	}

	/**
	 * @return the initialEvent
	 */
	public T getInitialEvent() {
		return initialEvent;
	}
}
