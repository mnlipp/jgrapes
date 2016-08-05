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
package org.jgrapes.io.events;

import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.Connection;

/**
 * Base class for events related to a {@link Connection}.
 * 
 * @param <T> the type of the event's result value (see {@link Event})
 * 
 * @author Michael N. Lipp
 */
public abstract class ConnectionEvent<T> 
	extends Event<T> {
	
	private Connection connection;

	/**
	 * @param connection
	 */
	public ConnectionEvent(Connection connection) {
		super();
		this.connection = connection;
	}

	/**
	 * @return the connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Fires the event as a response on its connection (see
	 * {@link Connection#respond(Event)}).
	 */
	public ConnectionEvent<T> fire() {
		return connection.respond(this);
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
			builder.append(Common.channelsToString(channels));
		}
		if (connection != null) {
			builder.append(", connection=");
			builder.append(Components.objectName(connection));
		}
		builder.append("]");
		return builder.toString();
	}

}
