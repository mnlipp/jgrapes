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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.HandlerScope;

/**
 * An variant of handler reference that provides better debug information
 * (at the cost of some cpu cycles).
 * 
 * @author Michael N. Lipp
 *
 */
class VerboseHandlerReference extends HandlerReference {

	private static AtomicLong invocationCounter = new AtomicLong(1);
	private ComponentType component;
	private String handlerName;
	
	/**
	 * @param component
	 * @param method
	 * @param eventParam
	 * @param priority
	 */
	public VerboseHandlerReference(ComponentType component, Method method, 
			int priority, HandlerScope filter) {
		super(component, method, priority, filter);
		this.component = component;
		handlerName = Components.objectName(component)
				+ "." + method.getName();
	}

	/**
	 * Invoke the handler with the given event as parameter. 
	 * 
	 * @param event the event
	 */
	@Override
	public void invoke(EventBase<?> event) throws Throwable {
		StringBuilder builder = new StringBuilder();
		long invocation = 0;
		if (handlerTracking.isLoggable(Level.FINEST)) {
			invocation = invocationCounter.getAndIncrement();
			builder.append('[');
			builder.append(Long.toString(invocation));
			builder.append("] ");
		}
		builder.append("P");
		builder.append(Components
		        .objectId(FeedBackPipelineFilter.getAssociatedPipeline()));
		builder.append(": ");
		builder.append(event);
		if (component == ComponentTree.DUMMY_HANDLER) {
			builder.append(" (unhandled)");
		} else {
			builder.append(" >> " + this);
		}
		handlerTracking.fine(builder.toString());
		super.invoke(event);
		if (handlerTracking.isLoggable(Level.FINEST)) {
			builder.setLength(0);
			builder.append("Result [");
			builder.append(Long.toString(invocation));
			builder.append("]: " + (event.getResult() == null ? "null"
			        : event.getResult()));
			handlerTracking.fine(builder.toString());
		}
	}

	@Override
	protected String methodToString() {
		return handlerName;
	}

}
