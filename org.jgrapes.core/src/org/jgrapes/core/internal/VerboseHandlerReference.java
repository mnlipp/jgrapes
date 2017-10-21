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

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.HandlerScope;

/**
 * An variant of handler reference that provides better debug information
 * (at the cost of some cpu cycles).
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
		if (component == ComponentTree.DUMMY_HANDLER) {
			reportInvocation(event, false);
			return;
		}
		long invocation;
		switch (method.type().parameterCount()) {
		case 0:
			// No parameters
			invocation = reportInvocation(event, false);
			method.invoke();
			reportResult(event, invocation);
			break;
			
		case 1:
			// Event parameter
			invocation = reportInvocation(event, false);
			method.invoke(event);
			reportResult(event, invocation);
			break;

		case 2:
			// Event and channel
			Class<?> channelParam = method.type().parameterType(1);
			boolean handlerFound = false;
			for (Channel channel: event.channels()) {
				if (channelParam.isAssignableFrom(channel.getClass())) {
					handlerFound = true;
					invocation = reportInvocation(event, false);
					method.invoke(event, channel);
					reportResult(event, invocation);
				}
			}
			if (!handlerFound) {
				reportInvocation(event, true);
			}
			break;
			
		default:
			throw new IllegalStateException("Handle not usable");
		}
	}

	private long reportInvocation(EventBase<?> event, boolean noChannel) {
		long invocation = 0; 
		StringBuilder builder = new StringBuilder();
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
			builder.append(" >> ");
			if (noChannel) {
				builder.append("No matching channels: ");
			}
			builder.append(this.toString());
		}
		String trackMsg = builder.toString();
		handlerTracking.fine(trackMsg);
		return invocation;
	}
	
	private void reportResult(EventBase<?> event, long invocation) {
		if (handlerTracking.isLoggable(Level.FINEST)) {
			StringBuilder builder = new StringBuilder();
			builder.append("Result [");
			builder.append(Long.toString(invocation));
			builder.append("]: " + (event.currentResults() == null ? "null"
			        : event.currentResults()));
			handlerTracking.fine(builder.toString());
		}
	}

	@Override
	protected String methodToString() {
		return handlerName;
	}

}
