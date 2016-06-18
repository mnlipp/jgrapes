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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.internal.Common;

/**
 * Signals the addition of a component (or subtree) to the component tree.
 * 
 * @author Michael N. Lipp
 */
public class Attached extends Event<Void> {

	private Component node;
	private Component parent;
	
	/**
	 * Creates a new event. The event is fired on both the
	 * node's and the parent's channel. If the channels are
	 * equal, the event is sent only once. If either component
	 * doesn't have a channel, the event is sent on the
	 * broadcast channel.
	 * 
	 * @param node the component being attached
	 * @param parent the component that the node is attached to
	 */
	public Attached(Component node, Component parent) {
		this.node = node;
		this.parent = parent;
	}

	/**
	 * Return the node that has been attached.
	 * 
	 * @return the node
	 */
	public Component getNode() {
		return node;
	}

	/**
	 * Return the parent component. When the root node is added to the 
	 * component tree, the parent is <code>null</code>.
	 * 
	 * @return the parent or <code>null</code>
	 */
	public Component getParent() {
		return parent;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Common.classToString(getClass()));
		builder.append('#');
		builder.append(Common.getId(getClass(), this));
		builder.append(" [");
		if (parent == null) {
			builder.append("ROOT");
		} else {
			builder.append(parent);
		}
		builder.append(" <―― ");
		builder.append(node);
		builder.append(", ");
		if (channels != null) {
			builder.append("channels=[");
			boolean first = true;
			for (Channel c: channels) {
				if (!first) {
					builder.append(", ");
				}
				builder.append(Common.channelKeyToString(c.getMatchKey()));
				first = false;
			}
			builder.append("]");
		}
		builder.append("]");
		return builder.toString();
	}
	
}
