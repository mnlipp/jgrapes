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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.Component;
import org.jgrapes.core.Channel;
import org.jgrapes.core.AttachedComponent;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.HandlingErrorPrinter;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.HandlingError;

/**
 * This class represents the component tree. It holds all properties that 
 * are common to all nodes of a component tree (the {@link ComponentVertex}s.
 * 
 * @author Michael N. Lipp
 */
class ComponentTree {

	private static final Logger handlerTracking 
		= Logger.getLogger(AttachedComponent.class.getPackage().getName() 
				+ ".handlerTracking");	
	
	private ComponentVertex root;
	private Map<EventChannelsTuple,HandlerList> handlerCache
		= new HashMap<EventChannelsTuple,HandlerList>();
	private InternalEventPipeline eventPipeline;
	private static HandlingErrorPrinter fallbackErrorHandler 
		= new HandlingErrorPrinter(); 
	public final static ComponentVertex DUMMY_HANDLER 
		= new Component(Channel.SELF) {
			@Handler(channels={Channel.class})
			public void noop(Event<?> event) {
			}
	};

	/**
	 * Creates a new tree for the given node or sub tree.
	 * 
	 * @param root the root node of the new tree
	 */
	ComponentTree(ComponentVertex root) {
		super();
		this.root = root;
	}

	ComponentVertex getRoot() {
		return root;
	}
	
	boolean isStarted() {
		return !(eventPipeline instanceof EventBuffer);
	}

	void setEventPipeline(InternalEventPipeline pipeline) {
		eventPipeline = pipeline;
	}
	
	InternalEventPipeline getEventPipeline() {
		return eventPipeline;
	}
	
	/**
	 * Forward to the thread's event manager.
	 * 
	 * @param event
	 * @param channels
	 */
	public void fire(Event<?> event, Channel[] channels) {
		eventPipeline.add(event, channels);
	}

	/**
	 * Merge all events stored with <code>source</code> with our own.
	 * This is invoked if a component (or component tree) is attached 
	 * to another tree (that uses this component common).
	 * 
	 * @param source
	 */
	void mergeEvents(ComponentTree source) {
		eventPipeline.merge(source.eventPipeline);
	}
	
	/**
	 * Send the event to all matching handlers.
	 * 
	 * @param event the event
	 * @param channels the channels the event is sent to
	 */
	void dispatch(EventPipeline pipeline, 
			EventBase<?> event, Channel[] channels) {
		HandlerList handlers = getEventHandlers(event, channels);
		handlers.process(pipeline, event);
	}
	
	private HandlerList getEventHandlers
		(EventBase<?> event, Channel[] channels) {
		EventChannelsTuple key = new EventChannelsTuple(event, channels);
		HandlerList hdlrs = handlerCache.get(key);
		if (hdlrs != null) {
			return hdlrs;
		}
		hdlrs = new HandlerList();
		root.collectHandlers(hdlrs, event, channels);
		// Make sure that errors are reported.
		if (hdlrs.isEmpty()) {
			if (event instanceof HandlingError) {
				((ComponentVertex)fallbackErrorHandler)
					.collectHandlers(hdlrs, event, channels);
			} else {
				if (handlerTracking.isLoggable(Level.FINER)) {
					DUMMY_HANDLER.collectHandlers(hdlrs, event, channels);
				}
			}
		}
		Collections.sort(hdlrs);
		handlerCache.put(key, hdlrs);
		return hdlrs;
	}
	
	void clearHandlerCache() {
		handlerCache.clear();
	}

}
