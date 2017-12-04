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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jgrapes.core.Event;
import org.jgrapes.portal.events.AddPageResources.ScriptResource;

/**
 * Adds a portlet type with its global resources (JavaScript and/or CSS) 
 * to the portal page. Specifying global resources result in the respective
 * `<link .../>` or `<script ...></script>` nodes
 * being added to the page's `<head>` node.
 * 
 * This in turn causes the browser to issue `GET` request that
 * (usually) refer to the portlet's resources. These requests are
 * converted to {@link PortletResourceRequest}s by the portal and
 * sent to the portlets, which must respond to these requests.
 * 
 * The complete sequence of events is shown in the diagram.
 * 
 * ![Portal Ready Event Sequence](AddPortletTypeSeq.svg)
 * 
 * Of course, due to internal buffering, the "Response Header" data
 * and the "Response body" data may collapse in a single message
 * that is sent to the browser (in case of a small resource).
 * 
 * @startuml AddPortletTypeSeq.svg
 * hide footbox
 * 
 * activate PortletX
 * PortletX -> Portal: AddPortletType 
 * deactivate PortletX
 * activate Portal
 * Portal -> Browser: "addPortletType"
 * activate Browser
 * deactivate Portal
 * deactivate Portal
 * Browser -> Portal: "GET <portlet resource1 URL>"
 * activate Portal
 * Portal -> PortletX: PortletResourceRequest
 * activate PortletX
 * PortletX -> Portal: PortletResourceResponse
 * Portal -> Browser: "Response Header"
 * deactivate Portal
 * loop until end of data
 *     PortletX -> Portal: Output
 *     activate Portal
 *     Portal -> Browser: "Response body"
 *     deactivate Portal
 * end loop
 * deactivate PortletX
 * deactivate Browser
 * 
 * @enduml
 */
public class AddPortletType extends Event<Void> {

	private String portletType;
	private String displayName = "";
	private boolean instantiable = false;
	private List<URI> cssUris = new ArrayList<>();
	private List<ScriptResource> scriptResources = new ArrayList<>();
	
	/**
	 * Create a new event for the given portlet type.
	 * 
	 * @param portletType a unique id for the portklet type (usually
	 * the class name)
	 */
	public AddPortletType(String portletType) {
		this.portletType = portletType;
	}
	
	/**
	 * Return the portlet type.
	 * 
	 * @return the portlet type
	 */
	public String portletType() {
		return portletType;
	}

	/**
	 * Sets the display name.
	 * 
	 * @param displayName the display name
	 * @return the event for easy chaining
	 */
	public AddPortletType setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}
	
	/**
	 * Return the display name.
	 * 
	 * @return the displayName
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Mark the portlet type as instantiable.
	 * 
	 * @return the event for easy chaining
	 */
	public AddPortletType setInstantiable() {
		instantiable = true;
		return this;
	}
	
	/**
	 * Return if the portelt is instantiable.
	 * 
	 * @return the result
	 */
	public boolean isInstantiable() {
		return instantiable;
	}

	/**
	 * Add a script resource to be requested by the browser.
	 * 
	 * @param scriptResource the script resource
	 * @return the event for easy chaining
	 */
	public AddPortletType addScript(ScriptResource scriptResource) {
		scriptResources.add(scriptResource);
		return this;
	}

	/**
	 * Add the URI of a CSS resource that is to be added to the
	 * header section of the portal page.
	 * 
	 * @param uri the URI
	 * @return the event for easy chaining
	 */
	public AddPortletType addCss(URI uri) {
		cssUris.add(uri);
		return this;
	}

	/**
	 * Return all script resources.
	 * 
	 * @return the result
	 */
	public ScriptResource[] scriptResources() {
		return scriptResources.toArray(new ScriptResource[scriptResources.size()]);
	}

	/**
	 * Return all CSS URIs.
	 * 
	 * @return the result
	 */
	public URI[] cssUris() {
		return cssUris.toArray(new URI[cssUris.size()]);
	}
}
