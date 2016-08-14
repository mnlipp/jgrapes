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
package org.jgrapes.core;

import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.internal.Common;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.core.internal.Matchable;

/**
 * The base class for all events. Event classes form a hierarchy.
 * By default (i.e. as implemented by this class), the event's class 
 * (type) is used for matching. A handler is invoked if the class of the
 * event handled by it is equal to or a base class of the class of the event 
 * to be handled. This default behavior can be changed by overriding the methods
 * from {@link Matchable}. See {@link NamedEvent} as an example.
 * 
 * @param <T> the result type of the event. Use {@link Void} if handling
 * the event does not produce a result
 * 
 * @author Michael N. Lipp
 */
public class Event<T> extends EventBase<T> {

	/**
	 * Creates a new event. Passing channels is equivalent to first
	 * creating the event and then calling {@link #setChannels(Channel...)}
	 * with the given channels.
	 * 
	 * @param channels the channels to set
	 */
	public Event(Channel... channels) {
		super();
		if (channels.length > 0) {
			setChannels(channels);
		}
	}

	/**
	 * Creates a new event. Sets the associated completed event to the
	 * event passed as parameter and sets this event as result of
	 * the completed event.
	 * <P>
	 * The channels are handled as in {@link Event#Event(Channel...)}.
	 * 
	 * @param channels the channels to set
	 */
	public Event(CompletedEvent<? extends Event<T>> completedEvent,
			Channel... channels) {
		super();
		@SuppressWarnings("unchecked")
		Event<Event<T>> ce = (Event<Event<T>>)completedEvent;
		ce.setResult(this);
		addCompletedEvent(ce);
		if (channels.length > 0) {
			setChannels(channels);
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.core.internal.Matchable#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return getClass();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return Class.class.isInstance(handlerKey)
				&& ((Class<?>)handlerKey).isAssignableFrom(getClass());
	}

	/**
	 * Implements the default behavior for handling events thrown
	 * by a handler. Fires a {@link HandlingError handling error} event
	 * for this event and the given throwable.
	 * 
	 * @see HandlingError
	 */
	@Override
	protected void handlingError
		(EventPipeline eventProcessor, Throwable throwable) {
		eventProcessor.fire
			(new HandlingError(this, throwable), channels());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Common.channelsToString(channels));
		}
		builder.append("]");
		return builder.toString();
	}

}
