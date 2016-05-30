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
 * Signals the removal of a component from the component tree.
 * This event is fired on both the node's and the parent's
 * channels.
 * 
 * @author Michael N. Lipp
 */
public class Detached extends Event {

	private Component node;
	private Component parent;
	
	/**
	 * Creates a new event.
	 * 
	 * @param node the component being removed
	 * @param parent the component that the node is removed from
	 */
	public Detached(Component node, Component parent) {
		this.parent = parent;
		this.node = node;
	}

	/**
	 * @return the node
	 */
	public Component getNode() {
		return node;
	}

	/**
	 * @return the parent
	 */
	public Component getParent() {
		return parent;
	}

}
