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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jdrupes.Component;
import org.jdrupes.Event;

/**
 * @author mnl
 *
 */
public class HandlerReference {

	private Object eventKey;
	private Object channelKey;
	private MethodHandle method;
	
	/**
	 * @param eventKey
	 * @param channelKey
	 * @param method
	 */
	public HandlerReference(Object eventKey, Object channelKey,	
			Component component, Method method) {
		super();
		this.eventKey = eventKey;
		this.channelKey = channelKey;
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
	 * Invoke the handler with the given event as parameter. 
	 * 
	 * @param event the event
	 */
	public void invoke(EventBase event) {
		try {
			method.invoke(event);
		} catch (Throwable e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return method.hashCode();
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
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		return true;
	}
	
	
	
}
