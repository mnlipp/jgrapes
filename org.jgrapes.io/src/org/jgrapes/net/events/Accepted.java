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

package org.jgrapes.net.events;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SNIServerName;

import org.jgrapes.core.Components;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.events.Opened;

/**
 * This event signals that a new connection has been made by a client.
 */
public class Accepted extends Opened {

	private SocketAddress localAddress;
	private SocketAddress remoteAddress;
	private boolean secure;
	private List<SNIServerName> requestedServerNames;

	/**
	 * Creates a new instance.
	 * 
	 * @param localAddress the local address
	 * @param remoteAddress the remote address
	 * @param secure indicates that this is a secure connection
	 * @param requestedServerNames the requested server names
	 * (in case of a TLS connection)
	 */
	public Accepted(SocketAddress localAddress, SocketAddress remoteAddress,
			boolean secure, List<SNIServerName> requestedServerNames) {
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.secure = secure;
		this.requestedServerNames 
			= Collections.unmodifiableList(requestedServerNames);
	}

	/**
	 * @return the localAddress
	 */
	public SocketAddress localAddress() {
		return localAddress;
	}

	/**
	 * @return the remoteAddress
	 */
	public SocketAddress remoteAddress() {
		return remoteAddress;
	}

	/**
	 * @return if this is a secure connection
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * @return the requested server names
	 */
	public List<SNIServerName> requestedServerNames() {
		return requestedServerNames;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		builder.append(localAddress);
		builder.append(" <― ");
		builder.append(remoteAddress);
		builder.append(", ");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Common.channelsToString(channels));
		}
		builder.append(", secure=");
		builder.append(secure);
		builder.append("]");
		return builder.toString();
	}
}
