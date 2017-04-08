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

import java.util.ArrayDeque;
import java.util.Arrays;

import org.jgrapes.core.Channel;

/**
 * This class provides a queue for events and the channels that they have
 * been fired on.
 */
@SuppressWarnings("serial")
class EventQueue extends ArrayDeque<EventChannelsTuple> {

	/**
	 * Convenience method that creates a {@link EventChannelsTuple}
	 * from the parameters and adds it to the queue.
	 * 
	 * @param event the event
	 * @param channels the channels
	 */
	public void add(EventBase<?> event, Channel[] channels) {
		add(new EventChannelsTuple(event, channels));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Arrays.toString(toArray());
	}
	
	
}
