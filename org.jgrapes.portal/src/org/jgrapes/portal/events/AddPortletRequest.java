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

import static org.jgrapes.portal.Portlet.*;

import org.jgrapes.portal.RenderSupport;


/**
 * Sent to the portal (server) for adding a new portlet instance of a given 
 * type. The portal usually responds with a {@link RenderPortlet}.
 * 
 * ![Event Sequence](AddPortletRequestSeq.svg)
 * 
 * @startuml AddPortletRequestSeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "addPortlet"
 * activate Portal
 * Portal -> Portlet: AddPortletRequest
 * deactivate Portal
 * activate Portlet
 * Portlet -> Portal: RenderPortlet
 * deactivate Portlet
 * activate Portal
 * Portal -> Browser: "renderPortlet"
 * deactivate Portal
 * 
 * @enduml
 * 
 */
public class AddPortletRequest extends Event<Void> {

	private RenderSupport renderSupport;
	private String portletType;
	private RenderMode renderMode;

	/**
	 * Creates a new event.
	 * 
	 * @param portletType the type of the portlet
	 * @param mode the view mode that is requested
	 */
	public AddPortletRequest(
			RenderSupport renderSupport, String portletType, RenderMode mode) {
		this.renderSupport = renderSupport;
		this.portletType = portletType;
		this.renderMode = mode;
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
	 * Returns the portlet type
	 * 
	 * @return the portlet type
	 */
	public String portletType() {
		return portletType;
	}

	/**
	 * Returns the render mode.
	 * 
	 * @return the render mode
	 */
	public RenderMode renderMode() {
		return renderMode;
	}
}
