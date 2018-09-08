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
 * Signals the removal of a component from the component tree.
 * This event is fired on both the node's and the parent's
 * channels.
 */
public class Detached extends Event<Void> {

    private final ComponentType node;
    private final ComponentType parent;

    /**
     * Creates a new event.
     * 
     * @param node the component being removed
     * @param parent the component that the node is removed from
     */
    public Detached(ComponentType node, ComponentType parent) {
        this.parent = parent;
        this.node = node;
    }

    /**
     * @return the node
     */
    public ComponentType node() {
        return node;
    }

    /**
     * @return the parent
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
        StringBuilder builder = new StringBuilder();
        builder.append(Components.objectName(this))
            .append(" [")
            .append(parent)
            .append(" <―/― ")
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
