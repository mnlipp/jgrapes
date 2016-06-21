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

import java.net.SocketAddress;

import org.jgrapes.core.Utils;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.Connection;

/**
 * Signals that a server has bound to a socket address and
 * is ready to accept connections.
 * 
 * @author Michael N. Lipp
 */
public class Ready extends Opened<Connection> {

	private SocketAddress listenAddress;

	/**
	 * Creates a new event.
	 * 
	 * @param connection
	 * @param socketAddress
	 */
	public Ready(Connection connection, SocketAddress socketAddress) {
		super(connection);
		this.listenAddress = socketAddress;
	}

	/**
	 * The address that the server has bound to.
	 * 
	 * @return the address
	 */
	public SocketAddress getListenAddress() {
		return listenAddress;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Utils.objectName(this));
		builder.append(" [");
		builder.append(listenAddress);
		builder.append(", ");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Common.channelsToString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
