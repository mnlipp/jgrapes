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

/**
 * Send to the portal for adding or updating a portlet. The content
 * is provided as string.
 */
public class RenderPortletFromString extends RenderPortlet {

	private String content;

	/**
	 * @param portletId
	 * @param title
	 * @param content
	 */
	public RenderPortletFromString(
			String portletId, String title, String content) {
		super(portletId, title);
		this.content = content;
	}

	/**
	 * @return the result
	 */
	public String content() {
		return content;
	}
}
