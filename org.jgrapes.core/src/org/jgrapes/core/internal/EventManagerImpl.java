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
package org.jgrapes.core.internal;

import java.util.Queue;

import org.jgrapes.core.Channel;

/**
 * @author mnl
 *
 */
public class EventManagerImpl implements EventManager {

	private ComponentTree componentCommon;
	private EventPipeline pipeline = null;
	private EventBase currentlyHandling = null;
	
	public EventManagerImpl (ComponentTree common) {
		this.componentCommon = common;
	}

	@Override
	public void fire(EventBase event, Channel... channels) {
		// Note the event currently being processed (if any) as cause
		((EventBase)event).generatedBy(currentlyHandling);
		if (pipeline != null) {
			// We have a queue, so the application has been started.
			// Simply add the event and the channels to the queue.
			pipeline.add(new EventChannelsTuple(event, channels));
			if (pipeline.size() > 1) {
				// This is a "nested" event (fired while processing an event).
				// It will be processed by the same thread that fired
				// the event which is currently being handled.
				return;
			}
		} else {
			// The application hasn't been started yet. Maybe we start it.
			pipeline = componentCommon.toBeProcessed
					(new EventChannelsTuple(event, channels));
			if (pipeline == null) {
				// We don't start it, i.e. the event fired is not the
				// StartEvent.
				return;
			}
		}
		// The event fired is either the StartEvent for a component tree,
		// or an arbitrary event and there has been a StartEvent before.
		// It's up to us to process the events on the queue
		while (pipeline.size() > 0) {
			EventChannelsTuple next = pipeline.peek();
			currentlyHandling = next.event;
			componentCommon.dispatch
				(this, currentlyHandling, next.channels);
			currentlyHandling.decrementOpen(this);
			pipeline.remove();
		}
		currentlyHandling = null;
	}
}
