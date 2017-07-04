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
 * A notification (as defined by the JSON RPC specification) to be sent to
 * the portlet view (the browser).
 */
public class DeletePortlet extends Event<Void> {

	private String portletId;
	
	/**
	 * Creates a new event.
	 *  
	 * @param portletId the portlet (view) that should be deleted
	 */
	public DeletePortlet(String portletId) {
		this.portletId = portletId;
	}

	/**
	 * Returns the portlet id.
	 * 
	 * @return the portlet id
	 */
	public String portletId() {
		return portletId;
	}

}
