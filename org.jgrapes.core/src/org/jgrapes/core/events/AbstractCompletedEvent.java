/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
 * @author mnl
 */
public abstract class AbstractCompletedEvent extends Event {
	private Event completedEvent;

	/**
	 * Convenience method that creates a completed event for the given
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
	 * @param completedEvent
	 */
	protected AbstractCompletedEvent(Event completedEvent) {
		super();
		this.completedEvent = completedEvent;
	}

	/**
	 * @return the completedEvent
	 */
	public Event getCompletedEvent() {
		return completedEvent;
	}
}
