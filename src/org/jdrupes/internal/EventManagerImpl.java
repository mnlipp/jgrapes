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

import java.util.LinkedList;
import java.util.Queue;

import org.jdrupes.Channel;
import org.jdrupes.Event;

/**
 * @author mnl
 *
 */
public class EventManagerImpl implements EventManager {

	private static class QueueEntry {
		public Event event;
		public Channel[] channels;
		public QueueEntry(Event event, Channel... channels) {
			this.event = event;
			this.channels = channels;
		}
	}
	
	private ComponentNode componentTree;
	private Queue<QueueEntry> queue = new LinkedList<QueueEntry>();
	private EventBase currentlyHandling = null;
	
	public EventManagerImpl (ComponentNode componentTree) {
		this.componentTree = componentTree;
	}

	@Override
	public void fire(Event event, Channel... channels) {
		boolean firstEvent = (queue.size() == 0);
		if (currentlyHandling == null) {
			event.setCausedBy(event);
		} else {
			event.setCausedBy(currentlyHandling);
		}
		queue.add(new QueueEntry(event, channels));
		if (firstEvent) {
			while (queue.size() > 0) {
				QueueEntry next = queue.remove();
				currentlyHandling = next.event;
				componentTree.dispatch(next.event, next.channels);
				next.event.decrementOpen(this);
			}
			currentlyHandling = null;
		}
	}
}
