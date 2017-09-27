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

package org.jgrapes.portal.events;

import org.jgrapes.core.Event;

/**
 * A response to a {@link PortletResourceRequest}. See
 * the description of {@link AddPortletType} for details. 
 */
public class PortletResourceResponse extends Event<Void> {

	private PortletResourceRequest request;

	/**
	 * Creates a new response.
	 * 
	 * @param request the request
	 * @param stream the data stream to be sent to the portal view (browser)
	 */
	public PortletResourceResponse(PortletResourceRequest request) {
		this.request = request;
	}

	/**
	 * Returns the request.
	 * 
	 * @return the request
	 */
	public PortletResourceRequest request() {
		return request;
	}

}
