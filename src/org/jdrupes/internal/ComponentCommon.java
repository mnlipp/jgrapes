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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jdrupes.Channel;
import org.jdrupes.Event;

/**
 * This class hold all properties that are common to all nodes
 * of a component tree.
 * 
 * @author mnl
 */
class ComponentCommon {

	public ComponentNode root;
	public Map<EventChannelTuple,Set<HandlerReference>> handlerCache
		= new HashMap<EventChannelTuple,Set<HandlerReference>>();

	/**
	 * @param root
	 */
	public ComponentCommon(ComponentNode root) {
		super();
		this.root = root;
	}

	public Set<HandlerReference> getHandlers
		(Event event, Channel[] channels) {
		EventChannelTuple key = new EventChannelTuple(event, channels);
		Set<HandlerReference> hdlrs = handlerCache.get(key);
		if (hdlrs != null) {
			return hdlrs;
		}
		hdlrs = new HashSet<>();
		root.addHandlers(hdlrs, event, channels);
		handlerCache.put(key, hdlrs);
		return hdlrs;
	}

	private static class EventChannelTuple {
		public Event event;		
		public Channel[] channels;
		
		/**
		 * @param event
		 * @param channels
		 */
		public EventChannelTuple(Event event, Channel[] channels) {
			super();
			this.event = event;
			this.channels = channels;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(channels);
			result = prime * result + ((event == null) ? 0 : event.hashCode());
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
			EventChannelTuple other = (EventChannelTuple) obj;
			if (!Arrays.equals(channels, other.channels))
				return false;
			if (event == null) {
				if (other.event != null)
					return false;
			} else if (!event.equals(other.event))
				return false;
			return true;
		}
	}
}
