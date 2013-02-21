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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdrupes.Event;
import org.jdrupes.Handler;

/**
 * @author mnl
 *
 */
public class EventManagerImpl {

	private ComponentNode componentTree = null; 
	private List<Event> queue = new LinkedList<Event>();
	private Map<HandlerKey, List<Handler>> handlerCache 
		= new HashMap<HandlerKey, List<Handler>>();
	
	public EventManagerImpl (ComponentNode componentTree) {
		this.componentTree = componentTree;
	}

	public void transferAll(EventManagerImpl eventManager) {
		queue.addAll(eventManager.queue);
		eventManager.queue.clear();
	}

	public void add(Event event) {
		queue.add(event);
	}
	
	private static class HandlerKey {
		public Class<Event> eventType;		
		public String channel;
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((channel == null) ? 0 : channel.hashCode());
			result = prime * result
					+ ((eventType == null) ? 0 : eventType.hashCode());
			return result;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HandlerKey other = (HandlerKey) obj;
			if (channel == null) {
				if (other.channel != null)
					return false;
			} else if (!channel.equals(other.channel))
				return false;
			if (eventType == null) {
				if (other.eventType != null)
					return false;
			} else if (!eventType.equals(other.eventType))
				return false;
			return true;
		}
		
	}
}
