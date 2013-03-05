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
package org.jdrupes.internal;

import java.util.Queue;

import org.jdrupes.Channel;
import org.jdrupes.Event;

/**
 * @author mnl
 *
 */
public class EventManagerImpl implements EventManager {

	private ComponentCommon componentCommon;
	private Queue<EventChannelsTuple> queue = null;
	private EventBase currentlyHandling = null;
	
	public EventManagerImpl (ComponentCommon common) {
		this.componentCommon = common;
	}

	@Override
	public void fire(Event event, Channel... channels) {
		((EventBase)event).enqueued(currentlyHandling);
		if (queue != null) {
			// the application is running
			queue.add(new EventChannelsTuple(event, channels));
			if (queue.size() > 1) {
				// this is a "nested" event (fired while processing an event)
				return;
			}
		} else {
			// the application hasn't been started yet. Maybe we start it.
			queue = componentCommon.toBeProcessed
					(new EventChannelsTuple(event, channels));
			if (queue == null) {
				return;
			}
		}
		// it's up to us to process the events on the queue
		while (queue.size() > 0) {
			EventChannelsTuple next = queue.peek();
			currentlyHandling = next.event;
			componentCommon.dispatch(currentlyHandling, next.channels);
			currentlyHandling.decrementOpen(this);
			queue.remove();
		}
		currentlyHandling = null;
	}
}
