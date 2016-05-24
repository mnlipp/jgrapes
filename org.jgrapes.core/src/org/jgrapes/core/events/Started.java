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

/**
 * The event that signals the completion of the {@link Start} event.
 * 
 * @author Michael N. Lipp
 */
public class Started extends AbstractCompletedEvent {

	/**
	 * An event for signaling the completion of the application start.
	 * <P>
	 * This event should be used by all components that autonomously
	 * (i.e. outside an event handler) produce events to begin their work.
	 * 
	 * @param completedEvent
	 */
	public Started(Start completedEvent) {
		super(completedEvent);
	}

}
