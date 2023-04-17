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

package org.jgrapes.net.events;

import java.net.SocketAddress;
import org.jgrapes.io.events.OpenSocketConnection;

/**
 * This event signals that a new connection has been made by a client.
 */
@SuppressWarnings("PMD.DataClass")
public class ClientConnected extends Connected<OpenSocketConnection> {

    /**
     * Creates a new instance.
     *
     * @param openEvent the open event
     * @param localAddress the local address
     * @param remoteAddress the remote address
     * (in case of a TLS connection)
     */
    public ClientConnected(OpenSocketConnection openEvent,
            SocketAddress localAddress, SocketAddress remoteAddress) {
        super(localAddress, remoteAddress);
        setResult(openEvent);
    }

    /**
     * Returns the event that caused this connection to be established.
     * 
     * @return the event
     */
    public OpenSocketConnection openEvent() {
        return currentResults().get(0);
    }
}
