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

import org.jgrapes.core.Event;
import org.jgrapes.io.Connection;
import org.jgrapes.io.DataConnection;

/**
 * Base class for events related to a {@link Connection}.
 * 
 * @author Michael N. Lipp
 */
public abstract class ConnectionEvent<T, C extends Connection> 
	extends Event<T> {
	
	private C connection;

	/**
	 * @param connection
	 */
	public ConnectionEvent(C connection) {
		super();
		this.connection = connection;
	}

	/**
	 * @return the connection
	 */
	public C getConnection() {
		return connection;
	}

	/**
	 * Fires the event as a response on its connection (see
	 * {@link Connection#respond(Event)}).
	 */
	public ConnectionEvent<T, C> fire() {
		return connection.respond(this);
	}
}
