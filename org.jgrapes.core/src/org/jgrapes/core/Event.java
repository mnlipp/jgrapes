/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
import org.jgrapes.core.internal.EventBase;

/**
 * This class is the base class for all events.
 *  
 * By default (i.e. as implemented by this class), the event's kind is
 * represented by its Java class and the eligibility is based on
 * the "is a" relationship between classes. An event is eligible if its class
 * is equal to or a super class of the class used as criterion. 
 * This default behavior can be changed by overriding the
 * methods from {@link Eligible}. See {@link NamedEvent} as an example.
 * 
 * @param <T>
 *            the result type of the event. Use {@link Void} if handling the
 *            event does not produce a result
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
	 * Returns the class of this event as representation of its kind.
	 * 
	 * @return the class of this event
	 * 
	 * @see org.jgrapes.core.Eligible#defaultCriterion()
	 */
	@Override
	public Object defaultCriterion() {
		return getClass();
	}

	/**
	 * Returns <code>true</code> if the `criterion` 
	 * is of the same class or a base class of this event's class.
	 * 
	 * @see org.jgrapes.core.Eligible#isEligibleFor(java.lang.Object)
	 */
	@Override
	public boolean isEligibleFor(Object criterion) {
		return Class.class.isInstance(criterion)
				&& ((Class<?>)criterion).isAssignableFrom(getClass());
	}

	/**
	 * Implements the default behavior for handling events thrown
	 * by a handler. Fires a {@link HandlingError handling error} event
	 * for this event and the given throwable.
	 * 
	 * @see HandlingError
	 */
	@Override
	protected void handlingError(
			EventPipeline eventProcessor, Throwable throwable) {
		eventProcessor.fire(
				new HandlingError(this, throwable), channels());
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
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
	}

}