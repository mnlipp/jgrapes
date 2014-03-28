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
package org.jdrupes.core.internal;

import java.awt.EventQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.jdrupes.core.Channel;
import org.jdrupes.core.events.Start;

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
	/** A non-null value indicates that no Started event has been 
	 * received yet. */
	private Queue<EventChannelsTuple> eventBuffer;
	/** The event manager that we delegate to. */
	private ThreadLocal<EventManager> eventManager
		= new ThreadLocal<EventManager>();

	/**
	 * Creates a new common object for the given mode or tree.
	 * 
	 * @param root
	 */
	ComponentCommon(ComponentNode root) {
		super();
		this.root = root;
		// Check whether common is created due to detach
		if (root.getCommon() == null) {
			// Newly created node
			eventBuffer = new ArrayDeque<>();
			return;
		}
		// Node already has common, so it is being detached from a tree
		if (root.getCommon().eventBuffer != null) {
			// Tree has an event buffer, so it hasn't been started yet
			throw new IllegalStateException
				("Components may not be detached from a tree before"
				 + " a Start event has been fired on it.");
		}
		// Detaching from a tree that has been started. Detached
		// node or subtree keeps that started state.
		eventBuffer = null;
	}

	ComponentNode getRoot() {
		return root;
	}

	boolean isStarted() {
		return eventBuffer == null;
	}
	
	/**
	 * Forward to the thread's event manager.
	 * 
	 * @param event
	 * @param channels
	 */
	public void fire(EventBase event, Channel[] channels) {
		EventManager em = eventManager.get();
		if (em == null) {
			em = new EventManagerImpl(this);
			eventManager.set(em);
		}
		em.fire(event, channels);
	}

	/**
	 * Merge all events stored with <code>source</code> with our own.
	 * This is invoked if a component (or component tree) is attached 
	 * to another tree (that uses this component common).
	 * <P>
	 * If both trees have not been started yet, events are simply
	 * appended to our queue.
	 * 
	 * @param source
	 */
	void mergeEvents(ComponentCommon source) {
		if (eventBuffer != null && source.eventBuffer != null) {
			eventBuffer.addAll(source.eventBuffer);
			return;
		}
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

	/**
	 * Invoked to check whether event processing should start
	 * for the component tree with this common object.
	 * <P>
	 * If event processing has been started, this method returns
	 * a <code>Queue</code> with the <code>queueItem</code> as
	 * single entry.
	 * If event processing hasn't been started
	 * yet for the tree and the <code>queueItem</code>
	 * contains a {@link de.jdrupes.events.Started} event 
	 * this method returns a queue with all previously buffered items
	 * and the given <code>queueItem</code>. 
	 * Else, this method bufferes the <code>queueItem</code>
	 * and returns <code>null</code>.
	 * 
	 * @param queueItem
	 * @return
	 */
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
