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
			this.method.bindTo(component);
		} catch (IllegalAccessException e) {
			throw (RuntimeException)
				(new IllegalArgumentException()).initCause(e);
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
	
	
}
