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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 * An event that signals the start of the application.
 * This event must be fired in order to start the dispatching of
 * events to components.
 * 
 * @author Michael N. Lipp
 */
public class Start extends Event {
	
	/**
	 * Create a new start event and set its completion event to {@Started}.
	 * The completion event may not be changed. The event's channels
	 * are set to {@link Channel#BROADCAST}.
	 */
	public Start() {
		setChannels(Channel.BROADCAST);
		setCompletedEvent(new Started(this));
	}

}
