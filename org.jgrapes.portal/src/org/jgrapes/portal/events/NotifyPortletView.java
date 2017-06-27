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
 * 
 */
public class NotifyPortletView extends Event<Void> {

	private String portletClass;
	private String portletId;
	private String method;
	private Object[] params;
	
	/**
	 * @param channels
	 * @param portletClass
	 * @param portletId
	 * @param method
	 * @param params
	 */
	public NotifyPortletView(String portletClass,
	        String portletId, String method, Object... params) {
		this.portletClass = portletClass;
		this.portletId = portletId;
		this.method = method;
		this.params = params;
	}

	/**
	 * @return the portletClass
	 */
	public String portletClass() {
		return portletClass;
	}

	/**
	 * @return the portletId
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * @return the method
	 */
	public String method() {
		return method;
	}

	/**
	 * @return the params
	 */
	public Object[] params() {
		return params;
	}
	

}
