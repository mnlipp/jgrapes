/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.http.events;

import java.net.SocketAddress;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.net.events.Connected;

/**
 * Signals that a TCP connection for a {@link HttpRequest} has been
 * established. The event is delivered on the subchannel that has been
 * created for handling the request.
 */
public class HttpConnected extends Connected {

    private Request.Out request;

    /**
     * Instantiates a new event.
     *
     * @param request the request
     * @param localAddress the local address
     * @param remoteAddress the remote address
     */
    public HttpConnected(Request.Out request, SocketAddress localAddress,
            SocketAddress remoteAddress) {
        super(localAddress, remoteAddress);
        this.request = request;
    }

    /**
     * Returns the request. The object returned is the object from the
     * original {@link Request.Out} event.
     *
     * @return the request
     */
    public Request.Out request() {
        return request;
    }

}
