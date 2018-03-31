/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.http.events;

import java.net.URI;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Event;
import org.jgrapes.http.HttpServer;

/**
 * Indicates that a component has accepted a {@link GetRequest} with
 * a header that requested an upgrade to another protocol.
 * 
 * The {@link HttpServer} component listens for such events and 
 * automatically creates the required {@link Response} event.
 */
public class ProtocolSwitchAccepted extends Event<Void> {

	private Request requestEvent;
	private String protocol;
	
	/**
	 * Creates a new event. The request event passed in as parameter
	 * is used by the {@link HttpServer} to build the response message
	 * and link an existing session to the web socket.
	 * 
	 * To be precise, the {@link HttpServer} retrieves the {@link HttpRequest}
	 * from the request event and uses the prepared response provided by 
	 * {@link HttpRequest#response()} to build the response. The default
	 * information contained in this prepared response is sufficient to
	 * build the actual response. If required, the accepting component 
	 * can add special header fields to the prepared response.
	 *
	 * @param request the base response data
	 * @param protocol the accepted protocol
	 */
	public ProtocolSwitchAccepted(Request request, String protocol) {
		this.requestEvent = request;
		this.protocol = protocol;
	}

	/**
	 * Returns the resource for which the socket was opened.
	 * 
	 * @return the value
	 */
	public URI resourceName() {
		return requestEvent.requestUri();
	}

	/**
	 * Returns the original request. 
	 * 
	 * @return the value
	 */
	public Request requestEvent() {
		return requestEvent;
	}
	
	/**
	 * The accepted protocol.
	 *
	 * @return the protocol
	 */
	public String protocol() {
		return protocol;
	}
}
