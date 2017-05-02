/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jgrapes.http.HttpServer;
import org.jgrapes.io.events.Opened;

/**
 * Indicates that a component has accepted a {@link GetRequest} with
 * a header that requested an upgrade to the WebSocket protocol.
 * 
 * The {@link HttpServer} component listens for such events and 
 * automatically creates the required {@link Response} event.
 */
public class WebSocketAccepted extends Opened {

	private URI resourceName;
	private HttpResponse baseResponse;
	
	/**
	 * Creates a new event. The base response passed in as parameter
	 * is used by the {@link HttpServer} to build the response message.
	 * 
	 * Usually, the `baseResponse` is the unmodified response provided
	 * by the request event (see {@link Request#request()} and 
	 * {@link HttpRequest#response()}). However, the accepting component 
	 * may add special header fields if required.
	 * 
	 * @param resourceName the resource referred to in the upgrade request
	 * @param baseResponse the base response data
	 */
	public WebSocketAccepted(URI resourceName, HttpResponse baseResponse) {
		this.baseResponse = baseResponse;
	}

	/**
	 * Returns the resource for which the socket was opened.
	 * 
	 * @return the value
	 */
	public URI resourceName() {
		return resourceName;
	}

	/**
	 * Returns the base response. 
	 * 
	 * @return the value
	 */
	public HttpResponse baseResponse() {
		return baseResponse;
	}
}
