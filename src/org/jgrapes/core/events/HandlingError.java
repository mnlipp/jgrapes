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

import org.jgrapes.core.Event;

/**
 * This event signals that a throwable occurred while
 * executing a handler.
 * 
 * @author mnl
 */
public class HandlingError extends Event {

	private Event event;
	private Throwable throwable;
	
	/**
	 * Creates a new event caused by the given throwable.
	 */
	public HandlingError(Event event, Throwable throwable) {
		this.event = event;
		this.throwable = throwable;
	}

	/**
	 * Returns the event that was dispatched when the error
	 * occurred.
	 * 
	 * @return the event
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * Returns the throwable that caused the generation of this event.
	 * 
	 * @return the throwable
	 */
	public Throwable getThrowable() {
		return throwable;
	}
}
