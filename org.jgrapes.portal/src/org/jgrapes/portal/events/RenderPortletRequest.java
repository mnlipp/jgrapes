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

import static org.jgrapes.portal.Portlet.*;

import org.jgrapes.portal.RenderSupport;

/**
 * Sent to the portal (server) if an existing portlet instance 
 * should be updated. The portal server usually responds with 
 * a {@link RenderPortlet} event that has as payload the
 * HTML that displays the portlet on the portal page.
 * 
 * ![Event Sequence](RenderPortletRequestSeq.svg)
 * 
 * @startuml RenderPortletRequestSeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "renderPortletRequest"
 * activate Portal
 * Portal -> Portlet: RenderPortletRequest
 * deactivate Portal
 * activate Portlet
 * Portlet -> Portal: RenderPortlet
 * deactivate Portlet
 * activate Portal
 * Portal -> Browser: "renderPortlet"
 * deactivate Portal
 * 
 * @enduml
 */
public class RenderPortletRequest extends RenderPortletRequestBase<Void> {

	private String portletId;
	private boolean foreground;

	/**
	 * Creates a new request.
	 * 
	 * @param renderSupport the render support for generating the response
	 * @param portletId the portlet to be updated
	 * @param renderMode the requested mode
	 */
	public RenderPortletRequest(RenderSupport renderSupport, 
			String portletId, RenderMode renderMode, boolean foreground) {
		super(renderSupport, renderMode);
		this.portletId = portletId;
		this.foreground = foreground;
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
	 * Indicates if portelt is to be put in foreground.
	 * 
	 * @return the result
	 */
	public boolean isForeground() {
		return foreground;
	}
}
