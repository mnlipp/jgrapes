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
package org.jgrapes.core.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jgrapes.core.Component;

/**
 * @author mnl
 *
 */
public class HandlerReference {

	private Object eventKey;
	private Object channelKey;
	private MethodHandle method;
	private int priority;
	
	/**
	 * @param eventKey
	 * @param channelKey
	 * @param method
	 * @param priority
	 */
	public HandlerReference(Object eventKey, Object channelKey,	
			Component component, Method method, int priority) {
		super();
		this.eventKey = eventKey;
		this.channelKey = channelKey;
		this.priority = priority;
		try {
			this.method = MethodHandles.lookup().unreflect(method);
			this.method = this.method.bindTo(component);
		} catch (IllegalAccessException e) {
			throw (RuntimeException)
				(new IllegalArgumentException
						("Method annotated as handler has wrong signature"
						 + " or class is not accessible"))
						.initCause(e);
		}
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
	public void invoke(EventBase event) throws Throwable {
		method.invoke(event);
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
}
