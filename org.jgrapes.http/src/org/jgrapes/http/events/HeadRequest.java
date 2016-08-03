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

import org.jdrupes.httpcodec.HttpRequest;
import org.jgrapes.core.Channel;
import org.jgrapes.io.Connection;

/**
 * @author Michael N. Lipp
 *
 */
public class HeadRequest extends Request {

	/**
	 * Create a new event.
	 * 
	 * @param connection the connection on which the event was received
	 * @param request the request
	 * @param channels the channels on which the event is to be 
	 * fired (optional)
	 */
	public HeadRequest(Connection connection,
	        HttpRequest request, Channel... channels) {
		super(connection, request, channels);
	}

}
