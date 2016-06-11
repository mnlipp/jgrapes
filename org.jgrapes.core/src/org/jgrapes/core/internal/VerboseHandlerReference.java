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

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.Component;

/**
 * An variant of handler reference that provides better debug information
 * (at the cost of some cpu cycles).
 * 
 * @author Michael N. Lipp
 *
 */
class VerboseHandlerReference extends HandlerReference {

	private static final Logger log 
		= Logger.getLogger(HandlerReference.class.getName());
	
	private String handlerName;
	
	/**
	 * @param eventKey
	 * @param channelKey
	 * @param component
	 * @param method
	 * @param eventParam
	 * @param priority
	 */
	public VerboseHandlerReference(Object eventKey, Object channelKey,
	        Component component, Method method, boolean eventParam,
	        int priority) {
		super(eventKey, channelKey, component, method, eventParam, priority);
		if (log.isLoggable(Level.FINER)) {
			handlerName = component.getClass().getName()
				+ "#" + method.getName();
		} else {
			handlerName = component.getClass().getSimpleName()
				+ "#" + method.getName();
		}
	}

	/**
	 * Invoke the handler with the given event as parameter. 
	 * 
	 * @param event the event
	 */
	@Override
	public void invoke(EventBase event) throws Throwable {
		handlerTracking.fine(event + " --> " + this);
		super.invoke(event);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Handler [");
		builder.append("method=");
		builder.append(handlerName);
		builder.append(", ");
		if (getEventKey() != null) {
			builder.append("event=");
			if (getEventKey() instanceof Class) {
				builder.append(((Class<?>) getEventKey()).getSimpleName());
			} else {
				builder.append(getEventKey());
			}
			builder.append(", ");
		}
		if (getChannelKey() != null) {
			builder.append("channel=");
			if (getChannelKey() instanceof Class) {
				builder.append(((Class<?>) getChannelKey()).getSimpleName());
			} else {
				builder.append(getChannelKey());
			}
			builder.append(", ");
		}
		builder.append("priority=");
		builder.append(getPriority());
		builder.append("]");
		return builder.toString();
	}
	
}
