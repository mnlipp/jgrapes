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
package org.jgrapes.core.events;

import org.jgrapes.core.Component;
import org.jgrapes.core.Event;

/**
 * Signals the addition of a component (or subtree) as child
 * of another component.
 * 
 * @author Michael N. Lipp
 */
public class Attached extends Event {

	private Component parent;
	private Component child;
	
	/**
	 * Creates a new event. The event is fired on both the
	 * parent's and the child's channel. If the channels are
	 * equal, the event is sent only once. If either component
	 * doesn't have a channel, the event is sent on the
	 * broadcast channel.
	 * 
	 * @param parent the component that the child is attached to
	 * @param child the component being attached
	 */
	public Attached(Component parent, Component child) {
		this.parent = parent;
		this.child = child;
	}

	/**
	 * @return the parent
	 */
	public Component getParent() {
		return parent;
	}

	/**
	 * @return the child
	 */
	public Component getChild() {
		return child;
	}

}
