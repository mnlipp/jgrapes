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

import org.jgrapes.portal.Portlet.RenderMode;
import org.jgrapes.portal.RenderSupport;


/**
 * Sent to the portal (server) if a new portlet instance of a given 
 * type should be added to the portal page. The portal server usually 
 * responds with a {@link RenderPortlet} event that has as payload the
 * HTML that displays the portlet on the portal page.
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
public class AddPortletRequest extends RenderPortletRequestBase {

	private String portletType;
	
	/**
	 * Creates a new event.
	 * 
	 * @param renderSupport the render support
	 * @param portletType the type of the portlet
	 * @param mode the view mode that is requested
	 */
	public AddPortletRequest(RenderSupport renderSupport, String portletType,
	        RenderMode mode) {
		super(renderSupport, mode);
		this.portletType = portletType;
	}

	/**
	 * Returns the portlet type
	 * 
	 * @return the portlet type
	 */
	public String portletType() {
		return portletType;
	}

}
