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

import java.util.Set;

import org.jgrapes.portal.Portlet.RenderMode;

/**
 * Send to the portal for adding or updating a portlet. The content
 * is provided as string.
 */
public class RenderPortletFromString extends RenderPortlet {

	private String content;

	/**
	 * Creates a new event.
	 * 
	 * @param portletClass the portlet class
	 * @param portletId the id of the portlet
	 * @param mode the view mode that is to be updated
	 * @param supportedModes the modes supported by the portlet
	 * @param content a string that defines the portlet view
	 * (as HTML)
	 * @param foreground if the portlet is to be put in the foreground
	 */
	public RenderPortletFromString(Class<?> portletClass,
			String portletId, RenderMode mode, Set<RenderMode> supportedModes,
			String content, boolean foreground) {
		super(portletClass, portletId, mode, supportedModes, foreground);
		this.content = content;
	}

	/**
	 * Returns the HTML that defines the portlet view.
	 * 
	 * @return the result
	 */
	public String content() {
		return content;
	}
}
