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

import java.util.Arrays;

/**
 * A class for events identified by name. Instances of this class 
 * represent events that use their name for matching 
 * events with handlers.
 * 
 * @author Michael N. Lipp
 */
final public class NamedEvent<T> extends Event<T> {

	private String name;
	
	/**
	 * Creates a new named event with the given name.
	 * 
	 * @param name the event's name
	 */
	public NamedEvent(String name) {
		super();
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Event#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return handlerKey.equals(Event.class) || handlerKey.equals(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("NamedEvent [name=");
		result.append(name);
		if (getChannels() != null) {
			result.append(", " + "channels=" + Arrays.toString(getChannels())); 
		}
		result.append("]");
		return result.toString();
	}
}
