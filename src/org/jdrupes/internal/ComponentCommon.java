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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jdrupes.Channel;
import org.jdrupes.events.Start;

/**
 * This class hold all properties that are common to all nodes
 * of a component tree.
 * 
 * @author mnl
 */
class ComponentCommon {

	private ComponentNode root;
	private Map<EventChannelsTuple,Set<HandlerReference>> handlerCache
		= new HashMap<EventChannelsTuple,Set<HandlerReference>>();
	private Queue<EventChannelsTuple> eventBuffer = new ArrayDeque<>();

	/**
	 * @param root
	 */
	ComponentCommon(ComponentNode root) {
		super();
		this.root = root;
	}

	ComponentNode getRoot() {
		return root;
	}
	
	/**
	 * Send the event to all matching handlers.
	 * 
	 * @param event the event
	 * @param channels the channels the event is sent to
	 */
	void dispatch(EventBase event, Channel[] channels) {
		Set<HandlerReference> hdlrs = getHandlers(event, channels);
		for (HandlerReference hdlr: hdlrs) {
			hdlr.invoke(event);
		}
	}
	
	private Set<HandlerReference> getHandlers
		(EventBase event, Channel[] channels) {
		EventChannelsTuple key = new EventChannelsTuple(event, channels);
		Set<HandlerReference> hdlrs = handlerCache.get(key);
		if (hdlrs != null) {
			return hdlrs;
		}
		hdlrs = new HashSet<>();
		root.collectHandlers(hdlrs, event, channels);
		handlerCache.put(key, hdlrs);
		return hdlrs;
	}
	
	void clearHandlerCache() {
		handlerCache.clear();
	}

	synchronized Queue<EventChannelsTuple> 
		toBeProcessed (EventChannelsTuple queueItem) {
		if (eventBuffer == null) {
			Queue<EventChannelsTuple> queue = new ArrayDeque<>();
			queue.add(queueItem);
			return queue;
		}
		if (queueItem.event instanceof Start) {
			Queue<EventChannelsTuple> queue = eventBuffer;
			eventBuffer = null;
			queue.add(queueItem);
			return queue;
		}
		eventBuffer.add(queueItem);
		return null;
	}
}
