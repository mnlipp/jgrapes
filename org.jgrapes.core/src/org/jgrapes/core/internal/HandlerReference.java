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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.AttachedComponent;

/**
 * A reference to a method that handles an event.
 * 
 * @author Michael N. Lipp
 */
class HandlerReference implements Comparable<HandlerReference> {

	protected static final Logger handlerTracking 
		= Logger.getLogger(AttachedComponent.class.getPackage().getName() 
			+ ".handlerTracking");
	
	private Object eventKey;
	private Object channelKey;
	private MethodHandle method;
	private boolean hasEventParam;
	private int priority;
	
	/**
	 * Create a new handler reference to a component's method that 
	 * handles the given kind of event on the given channel.
	 * 
	 * @param eventKey the kind of event handled
	 * @param channelKey the channel listening to
	 * @param method the method to be invoked
	 * @param eventParam {@code true} if the handler has an event parameter
	 * @param priority the handler's priority
	 */
	protected HandlerReference(Object eventKey, Object channelKey,	
			AttachedComponent component, Method method, boolean eventParam, 
			int priority) {
		super();
		this.eventKey = eventKey;
		this.channelKey = channelKey;
		this.hasEventParam = eventParam;
		this.priority = priority;
		try {
			this.method = MethodHandles.lookup().unreflect(method);
			this.method = this.method.bindTo(component);
		} catch (IllegalAccessException e) {
			throw (RuntimeException)
				(new IllegalArgumentException("Method "
						+ component.getClass().getName() 
						+ "." + method.getName()
						+ " annotated as handler has wrong signature"
						 + " or class is not accessible"))
						.initCause(e);
		}
	}

	@Override
	public int compareTo(HandlerReference o) {
		if (getPriority() < o.getPriority()) {
			return 1;
		}
		if (getPriority() > o.getPriority()) {
			return -1;
		}
		return 0;
	}

	/**
	 * @return the eventKey
	 */
	public Object getEventKey() {
		return eventKey;
	}
	
	/**
	 * @return the channelKey
	 */
	public Object getChannelKey() {
		return channelKey;
	}
	
	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}
	
	/**
	 * Invoke the handler with the given event as parameter. 
	 * 
	 * @param event the event
	 */
	public void invoke(EventBase<?> event) throws Throwable {
		if (hasEventParam) {
			method.invoke(event);
		} else {
			method.invoke();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((channelKey == null) ? 0 : channelKey.hashCode());
		result = prime * result
				+ ((eventKey == null) ? 0 : eventKey.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + priority;
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
		HandlerReference other = (HandlerReference) obj;
		if (channelKey == null) {
			if (other.channelKey != null)
				return false;
		} else if (!channelKey.equals(other.channelKey))
			return false;
		if (eventKey == null) {
			if (other.eventKey != null)
				return false;
		} else if (!eventKey.equals(other.eventKey))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (priority != other.priority)
			return false;
		return true;
	}
	
	protected String methodToString() {
		return method.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Handler [");
		if (method != null) {
			builder.append("method=");
			builder.append(methodToString());
			builder.append(", ");
		}
		if (eventKey != null) {
			builder.append("event=");
			if (eventKey instanceof Class) {
				builder.append(Common.classToString((Class<?>) eventKey));
			} else {
				builder.append(eventKey);
			}
			builder.append(", ");
		}
		if (channelKey != null) {
			builder.append("channel=");
			builder.append(Common.channelKeyToString(channelKey));
			builder.append(", ");
		}
		builder.append("priority=");
		builder.append(priority);
		builder.append("]");
		return builder.toString();
	}
	
	static abstract class HandlerRefFactory {
		abstract HandlerReference createHandlerRef
			(Object eventKey, Object channelKey,	
			 AttachedComponent component, Method method, boolean eventParam, 
			 int priority);
	}

    public static HandlerReference newRef(Object eventKey, Object channelKey,	
			AttachedComponent component, Method method, boolean eventParam, 
			int priority) {
    	if (handlerTracking.isLoggable(Level.FINE)) {
			return new VerboseHandlerReference(eventKey, channelKey,
	                component, method, eventParam, priority);
    	} else {
			return new HandlerReference(eventKey, channelKey,
	                component, method, eventParam, priority);
    	}
    }
}
