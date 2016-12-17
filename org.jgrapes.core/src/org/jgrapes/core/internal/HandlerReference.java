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

import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Criterion;
import org.jgrapes.core.Event;
import org.jgrapes.core.HandlerScope;

/**
 * A reference to a method that handles an event.
 * 
 * @author Michael N. Lipp
 */
class HandlerReference implements Comparable<HandlerReference> {

	protected static final Logger handlerTracking 
		= Logger.getLogger(ComponentType.class.getPackage().getName() 
			+ ".handlerTracking");
	
	private HandlerScope filter;
	private MethodHandle method;
	private boolean hasEventParam;
	private int priority;
	
	/**
	 * Create a new handler reference to a component's method that 
	 * handles events matching the filter.
	 * 
	 * @param component the component
	 * @param method the method to be invoked
	 * @param priority the handler's priority
	 * @param filter the filter
	 */
	protected HandlerReference(ComponentType component, Method method,	
			int priority, HandlerScope filter) {
		super();
		this.filter = filter;
		this.priority = priority;
		Class<?>[] paramTypes = method.getParameterTypes();
		hasEventParam = false;
		if (paramTypes.length > 0) {
			if (Event.class.isAssignableFrom(paramTypes[0])) {
				hasEventParam = true;
			}
		}
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
	 * Returns {@code true} if this handler handles the given event
	 * fired on the given channels. 
	 * 
	 * @param event the event
	 * @param channels the channels
	 * @return the result
	 */
	public boolean handles(Criterion event, Criterion[] channels) {
		return filter.includes(event, channels);
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

	protected String methodToString() {
		return method.toString();
	}

	static abstract class HandlerRefFactory {
		abstract HandlerReference createHandlerRef
			(Object eventKey, Object channelKey,	
			 ComponentType component, Method method, boolean eventParam, 
			 int priority);
	}

    public static HandlerReference newRef
    		(ComponentType component, Method method,
			int priority, HandlerScope filter) {
    	if (handlerTracking.isLoggable(Level.FINE)) {
			return new VerboseHandlerReference
					(component, method, priority, filter);
    	} else {
			return new HandlerReference(component, method, priority, filter);
    	}
    }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + (hasEventParam ? 1231 : 1237);
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
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (hasEventParam != other.hasEventParam)
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HandlerReference [");
		if (method != null) {
			builder.append("method=");
			builder.append(method);
			builder.append(", ");
		}
		if (filter != null) {
			builder.append("filter=");
			builder.append(filter);
			builder.append(", ");
		}
		builder.append("priority=");
		builder.append(priority);
		builder.append("]");
		return builder.toString();
	}
	
}
