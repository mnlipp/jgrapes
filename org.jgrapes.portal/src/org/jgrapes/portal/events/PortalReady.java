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
import org.jgrapes.portal.RenderSupport;

/**
 * Signals the successful loading of the portal structure
 * in the browser. Portlets should respond to this event by
 * adding their types (using {@link AddPortletType} events).
 * 
 * After the portal ready event has been processed, a {@link PortalPrepared}
 * event is generated as {@link CompletionEvent}. A portal policy component
 * should respond to this event and trigger the rendering of the portlets
 * that are to be displayed in the current portal state.
 * 
 * ![Event Sequence](PortalReadySeq.svg)
 * 
 * @startuml PortalReadySeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "portalReady"
 * activate Portal
 * Portal -> PortletA: PortalReady
 * activate PortletA
 * PortletA -> Portal: AddPortletType 
 * activate Portal
 * Portal -> Browser: "addPortletType"
 * deactivate Portal
 * 
 * loop for all remaining portlets
 *     Portal -> PortletX: PortalReady
 *     activate PortletX
 *     PortletX -> Portal: AddPortletType 
 *     activate Portal
 *     Portal -> Browser: "addPortletType"
 *     deactivate Portal
 * end
 * 
 * actor System
 * System -> PortalPolicy: PortalPrepared
 * deactivate Portal
 * activate PortalPolicy
 * PortalPolicy -> PortletA: RenderPortletRequest
 * deactivate PortalPolicy
 * activate PortletA
 * PortletA -> Portal: RenderPortlet
 * deactivate PortletA
 * activate Portal
 * Portal -> Browser: "renderPortlet"
 * deactivate Portal
 * 
 * @enduml
 */
public class PortalReady extends Event<Void> {

	private RenderSupport renderSupport;

	/**
	 * Creates a new event.
	 * 
	 * @param renderSupport the render support for generating responses
	 */
	public PortalReady(RenderSupport renderSupport) {
		new PortalPrepared(this);
		this.renderSupport = renderSupport;
	}

	/**
	 * Returns the render support.
	 * 
	 * @return the render support
	 */
	public RenderSupport renderSupport() {
		return renderSupport;
	}
}
