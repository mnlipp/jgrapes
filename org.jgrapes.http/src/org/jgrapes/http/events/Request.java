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
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.Connection;

/**
 * @author Michael N. Lipp
 *
 */
public class Request extends Event<Void> {

	public static class Completed extends CompletedEvent<Request> {
	}
	
	private HttpRequest request;
	private Connection connection;
	
	/**
	 * Creates a new request event with the associated {@link Completed}
	 * event.
	 * 
	 * @param connection the connection the request is associated with
	 * @param request the request data
	 * @param channels the channels associated with this event
	 */
	public Request(Connection connection, 
			HttpRequest request, Channel... channels) {
		super(new Completed(), channels);
		this.connection = connection;
		this.request = request;
	}

	/**
	 * Returns the connection.
	 * 
	 * @return the connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Returns the request.
	 * 
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [\"");
		String path = request.getRequestUri().getPath();
		if (path.length() > 15) {
			builder.append("...");
			builder.append(path.substring(path.length() - 12));
		} else {
			builder.append(path);
		}
		builder.append("\"");
		if (connection != null) {
			builder.append(">>P");
			builder.append(
			        Components.objectId(connection.getResponsePipeline()));
		}
		builder.append(", ");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Common.channelsToString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
