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
 * 
 */
public class AddPortletResources extends Event<Void> {

	private String portletType;
	private List<URI> scriptUris = new ArrayList<>();
	private List<URI> cssUris = new ArrayList<>();
	
	/**
	 * @param channels
	 * @param portletType
	 * @param scriptUri
	 */
	public AddPortletResources(String portletType) {
		this.portletType = portletType;
	}
	
	/**
	 * @return the portletType
	 */
	public String portletType() {
		return portletType;
	}

	public AddPortletResources addScript(URI uri) {
		scriptUris.add(uri);
		return this;
	}
	
	public AddPortletResources addCss(URI uri) {
		cssUris.add(uri);
		return this;
	}
	
	public URI[] scriptUris() {
		return scriptUris.toArray(new URI[scriptUris.size()]);
	}
	
	public URI[] cssUris() {
		return cssUris.toArray(new URI[cssUris.size()]);
	}
}
