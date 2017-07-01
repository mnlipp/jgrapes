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
public class NotifyPortletView extends Event<Void> {

	private String portletClass;
	private String portletId;
	private String method;
	private Object[] params;
	
	/**
	 * Creates a new event.
	 *  
	 * @param portletClass the portlet class (used to look up the available
	 * functions)
	 * @param portletId the portlet (view) that the notification is directed
	 * at
	 * @param method the method (function) to be executed
	 * @param params the parameters
	 */
	public NotifyPortletView(String portletClass,
	        String portletId, String method, Object... params) {
		this.portletClass = portletClass;
		this.portletId = portletId;
		this.method = method;
		this.params = params;
	}

	/**
	 * Returns the portlet class.
	 * 
	 * @return the portlet class
	 */
	public String portletClass() {
		return portletClass;
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
	 * Returns the method to be executed.
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
	public Object[] params() {
		return params;
	}
	

}
