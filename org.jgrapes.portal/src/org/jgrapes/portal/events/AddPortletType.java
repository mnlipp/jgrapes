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

/**
 * Adds a portlet type with its global resources (JavaScript and/or CSS) 
 * to the portal page.
 */
public class AddPortletType extends Event<Void> {

	private String portletType;
	private String displayName = "";
	private boolean instantiable = false;
	private List<URI> scriptUris = new ArrayList<>();
	private List<URI> cssUris = new ArrayList<>();
	
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
	 * Add the URI of a JavaScript resource that is to be added to the
	 * header section of the portal page.
	 * 
	 * @param uri the URI
	 * @return the event for easy chaining
	 */
	public AddPortletType addScript(URI uri) {
		scriptUris.add(uri);
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
	 * Return all script URIs
	 * 
	 * @return the result
	 */
	public URI[] scriptUris() {
		return scriptUris.toArray(new URI[scriptUris.size()]);
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
