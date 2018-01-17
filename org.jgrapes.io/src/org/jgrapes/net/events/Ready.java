/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.net.events;

import java.net.SocketAddress;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.io.events.Opened;

/**
 * Signals that a server has bound to a socket address and
 * is ready to accept connections.
 */
public class Ready extends Opened {

	private SocketAddress listenAddress;

	/**
	 * Creates a new event.
	 * 
	 * @param socketAddress the socket address
	 */
	public Ready(SocketAddress socketAddress) {
		this.listenAddress = socketAddress;
	}

	/**
	 * The address that the server has bound to.
	 * 
	 * @return the address
	 */
	public SocketAddress listenAddress() {
		return listenAddress;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		builder.append(listenAddress);
		builder.append(", ");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
