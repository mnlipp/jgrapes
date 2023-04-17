/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.net;

import java.net.SocketAddress;
import org.jgrapes.io.IOSubchannel;

/**
 * A special sub channel used for socket connections.
 */
public interface SocketIOChannel extends IOSubchannel {

    /**
     * Returns the local address.
     *
     * @return the socket address
     */
    SocketAddress localAddress();

    /**
     * Returns the remote address.
     *
     * @return the socket address
     */
    SocketAddress remoteAddress();

    /**
     * Checks if the connection is purgeable.
     *
     * @return true, if is purgeable
     */
    boolean isPurgeable();

    /**
     * Returns since when the connection has become purgeable.
     *
     * @return the timestamp
     */
    long purgeableSince();
}
