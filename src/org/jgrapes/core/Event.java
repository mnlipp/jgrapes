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
package org.jgrapes.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.jgrapes.core.events.AbstractCompletedEvent;
import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.core.internal.EventManager;
import org.jgrapes.core.internal.Matchable;

/**
 * The base class for all events. Event classes form a hierarchy.
 * By default (i.e. as implemented by this class), the event's class 
 * (type) is used for matching. A handler is invoked if its event 
 * class is equal to or a base class of the class of the event 
 * to be handled. 
 * <P>
 * This default behavior can be changed by overriding the methods
 * from {@link Matchable}. See {@link NamedEvent} as an example.
 * 
 * @author mnl
 */
public class Event extends EventBase {

	/**
	 * Returns the channels associated with the event. Before an
	 * event has been fired, this returns the channels set with
	 * {@link #setChannels(Channel[])}. After an event has been
	 * fired, this returns the channels that the event has
	 * effectively been fired on 
	 * (see {@link Manager#fire(Event, Channel...)}).
	 * 
	 * @return the channels
	 */
	public Channel[] getChannels() {
		return channels;
	}

	/**
	 * Sets the channels that the event is fired on if no channels
	 * are specified explicitly when firing the event
	 * (see {@link org.jgrapes.core.Manager#fire(Event, Channel...)}).
	 * 
	 * @param channels the channels to set
	 * 
	 * @throws IllegalStateException if the method is called after
	 * this event has been fired
	 */
	public void setChannels(Channel[] channels) {
		if (currentlyHandled()) {
			throw new IllegalStateException
				("Channels cannot be changed after fire");
		}
		this.channels = channels;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.MatchKeyProvider#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return getClass();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return Class.class.isInstance(handlerKey)
				&& ((Class<?>)handlerKey).isAssignableFrom(getClass());
	}

	public Event addCompletedEvent
		(Class<? extends AbstractCompletedEvent> clazz) {
		try {
			for (Constructor<?> c: clazz.getConstructors()) {
				if (c.getParameterTypes().length != 1) {
					continue;
				}
				if (!Event.class.isAssignableFrom(c.getParameterTypes()[0])) {
					continue;
				}
				setCompletedEvent ((Event)c.newInstance(this));
				return this;
			}
			throw new IllegalArgumentException
				("Class " + clazz.getName() + " has no <init>(Event)");
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException | SecurityException e) {
			throw (RuntimeException)
				(new IllegalArgumentException()).initCause(e);
		}
	}
	
	/**
	 * Implements the default behavior for handling events thrown
	 * by handler. Fires a {@link HandlingError handling error} event
	 * for this event and the given throwable.
	 * 
	 * @see HandlingError
	 */
	@Override
	protected void handlingError(EventManager mgr, Throwable throwable) {
		mgr.fire(new HandlingError(this, throwable), getChannels());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getMatchKey().hashCode();
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
		Event other = (Event) obj;
		if (getMatchKey() == null) {
			if (other.getMatchKey() != null)
				return false;
		} else if (!getMatchKey().equals(other.getMatchKey()))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() 
				+ " [matchKey=" + getMatchKey() + ", channels="
				+ Arrays.toString(getChannels()) + ", completedEvent="
				+ getCompletedEvent() + "]";
	}
	
	
}
