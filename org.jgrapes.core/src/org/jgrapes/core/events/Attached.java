/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.events;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * Signals the addition of a component (or subtree) to the component tree.
 */
public class Attached extends Event<Void> {

    private final ComponentType node;
    private final ComponentType parent;

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
    public Attached(ComponentType node, ComponentType parent) {
        this.node = node;
        this.parent = parent;
    }

    /**
     * Return the node that has been attached.
     * 
     * @return the node
     */
    public ComponentType node() {
        return node;
    }

    /**
     * Return the parent component. When the root node is added to the 
     * component tree, the parent is <code>null</code>.
     * 
     * @return the parent or <code>null</code>
     */
    public ComponentType parent() {
        return parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append(Components.objectName(this))
            .append(" [");
        if (parent == null) {
            builder.append("ROOT");
        } else {
            builder.append(parent);
        }
        builder.append(" <―― ")
            .append(node)
            .append(", ");
        if (channels().length > 0) {
            builder.append("channels=");
            builder.append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }

}
