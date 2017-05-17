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

import org.jgrapes.core.internal.Common;

/**
 * A base class for completion events. The completed event is the
 * result of the completion event. Use {@link #event()} to access
 * the completed event while handling the completion event. 
 */
public abstract class CompletedEvent<T extends Event<?>>
		extends Event<T> {

	public CompletedEvent(Channel... channels) {
		super(channels);
	}

	/**
	 * Return the completed event.
	 * 
	 * @return the completed event
	 */
	public T event() {
		return result();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Common.classToString(getClass()));
		builder.append("(");
		builder.append(Components.objectName(result()));
		builder.append(")");
		builder.append(" [");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
