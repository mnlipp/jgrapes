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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	private Map<EventChannelsTuple,List<HandlerReference>> handlerCache
		= new HashMap<EventChannelsTuple,List<HandlerReference>>();
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
	void dispatch(EventManager mgr, EventBase event, Channel[] channels) {
		List<HandlerReference> hdlrs = getHandlers(event, channels);
		for (HandlerReference hdlr: hdlrs) {
			try {
				hdlr.invoke(event);
			} catch (Throwable t) {
				event.handlingError(mgr, t);
			}
		}
	}
	
	private List<HandlerReference> getHandlers
		(EventBase event, Channel[] channels) {
		EventChannelsTuple key = new EventChannelsTuple(event, channels);
		List<HandlerReference> hdlrs = handlerCache.get(key);
		if (hdlrs != null) {
			return hdlrs;
		}
		hdlrs = new ArrayList<>();
		root.collectHandlers(hdlrs, event, channels);
		Collections.sort(hdlrs, new Comparator<HandlerReference>() {
			@Override
			public int compare(HandlerReference hr1, HandlerReference hr2) {
				return hr2.getPriority() - hr1.getPriority();
			}
		});
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
