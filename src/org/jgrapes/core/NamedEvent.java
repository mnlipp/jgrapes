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

/**
 * A class for events identified by name. Instances of this class 
 * represent events that use their name for matching 
 * events with handlers.
 * 
 * @author mnl
 */
final public class NamedEvent extends Event {

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
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
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
		return "NamedEvent [name=" + name + "]";
	}
}
