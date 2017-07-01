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

import javax.json.JsonArray;

import org.jgrapes.core.Event;
import org.jgrapes.portal.RenderSupport;

/**
 * A decoded notification (as defined by the JSON RPC specification) that
 * invokes a method on a portlet model.
 */
public class NotifyPortletModel extends Event<Void> {

	private RenderSupport renderSupport;
	private String portletId;
	private String method;
	private JsonArray params;
	
	/**
	 * Creates a new event.
	 * 
	 * @param renderSupport the render support from the portal in case
	 * the response requires it
	 * @param portletId the portlet model that the notification is 
	 * directed at 
	 * @param method the method to be executed
	 * @param params parameters
	 */
	public NotifyPortletModel(RenderSupport renderSupport, 
			String portletId, String method, JsonArray params) {
		this.renderSupport = renderSupport;
		this.portletId = portletId;
		this.method = method;
		this.params = params;
	}

	/**
	 * Returns the render support.
	 * 
	 * @return the render support
	 */
	public RenderSupport renderSupport() {
		return renderSupport;
	}

	/**
	 * Returns the portlet id.
	 * 
	 * @return the portlet id
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * Returns the method.
	 * 
	 * @return the method
	 */
	public String method() {
		return method;
	}

	/**
	 * Returns the parameters.
	 * 
	 * @return the parameters
	 */
	public JsonArray params() {
		return params;
	}
}
