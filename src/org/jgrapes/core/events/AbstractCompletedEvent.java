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
