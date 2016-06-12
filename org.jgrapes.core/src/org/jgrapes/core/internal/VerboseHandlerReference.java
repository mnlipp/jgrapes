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

import org.jgrapes.core.Component;
import org.jgrapes.core.EventPipeline;

/**
 * An variant of handler reference that provides better debug information
 * (at the cost of some cpu cycles).
 * 
 * @author Michael N. Lipp
 *
 */
class VerboseHandlerReference extends HandlerReference {

	private Component component;
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
		this.component = component;
		handlerName = Common.classToString(component.getClass())
				+ "#" + method.getName();
	}

	/**
	 * Invoke the handler with the given event as parameter. 
	 * 
	 * @param event the event
	 */
	@Override
	public void invoke(EventBase event) throws Throwable {
		StringBuilder builder = new StringBuilder();
		builder.append("P");
		builder.append(Common.getId(EventPipeline.class, 
				FeedBackPipelineFilter.getAssociatedPipeline()));
		builder.append(": ");
		builder.append(event);
		if (component == ComponentTree.DUMMY_HANDLER) {
			builder.append(" (unhandled)");
		} else {
			builder.append(" --> " + this);
		}
		handlerTracking.fine(builder.toString());
		super.invoke(event);
	}

	@Override
	protected String methodToString() {
		return handlerName;
	}

}
