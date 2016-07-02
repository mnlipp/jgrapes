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

import org.jdrupes.httpcodec.HttpResponse;
import org.jgrapes.core.Event;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * @author Michael N. Lipp
 *
 */
public class Response extends Event<Void> {

	private DataConnection<ManagedByteBuffer> connection;
	private HttpResponse response;
	
	public Response(DataConnection<ManagedByteBuffer> connection, 
			HttpResponse response) {
		this.connection = connection;
		this.response = response;
	}

	/**
	 * @return the connection
	 */
	public DataConnection<ManagedByteBuffer> getConnection() {
		return connection;
	}

	/**
	 * @return the response
	 */
	public HttpResponse getResponse() {
		return response;
	}

	
}
