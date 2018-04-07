/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Channel;

/**
 * Represents a HTTP GET request.
 */
public class GetRequest extends Request {

    /**
     * Create a new event.
     * 
     * @param request the request data
     * @param secure indicates whether the request was received on a
     * secure channel
     * @param matchLevels the number of elements from the request path
     * to use in the match value
     * @param channels the channels on which the event is to be 
     * fired (optional)
     */
    public GetRequest(HttpRequest request, boolean secure,
            int matchLevels, Channel... channels) {
        super(secure ? "https" : "http", request, matchLevels, channels);
    }

}
