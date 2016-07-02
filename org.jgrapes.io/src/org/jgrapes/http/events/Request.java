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
package org.jgrapes.http.events;

import java.net.URI;

import org.jdrupes.httpcodec.HttpRequest;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletedEvent;
import org.jgrapes.core.Event;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * @author Michael N. Lipp
 *
 */
public class Request extends Event<Request.HandlingResult> {

	public static enum HandlingResult { UNHANDLED, RESOURCE_NOT_FOUND,
		RESPONDED };
	
	public static class Completed extends CompletedEvent<Request> {
	}
	
	private HttpRequest request;
	private DataConnection<ManagedByteBuffer> connection;
	
	/**
	 * @param request the request data
	 * @param channels the channels associated with this event
	 */
	public Request(DataConnection<ManagedByteBuffer> connection, 
			HttpRequest request, Channel... channels) {
		super(new Completed(), channels);
		super.setResult(HandlingResult.UNHANDLED);
		this.connection = connection;
		this.request = request;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.EventBase#setResult(java.lang.Object)
	 */
	@Override
	public Event<HandlingResult> setResult(HandlingResult result) {
		if (getResult() == HandlingResult.UNHANDLED
				|| result == HandlingResult.RESPONDED) {
			return super.setResult(result);
		}
		return this;
	}

	/**
	 * @return the connection
	 */
	public DataConnection<ManagedByteBuffer> getConnection() {
		return connection;
	}

	/**
	 * @return the request
	 */
	public HttpRequest getRequest() {
		return request;
	}

	/**
	 * Shortcut for getting the request URI from the request. 
	 * 
	 * @return the request URI
	 */
	public URI getRequestUri() {
		return getRequest().getRequestUri();
	}
}
