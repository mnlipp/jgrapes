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
import org.jgrapes.core.Event;

import static org.jgrapes.portal.Portlet.*;

/**
 * Send to the portal for adding or updating a portlet.
 */
public abstract class RenderPortlet extends Event<Void> {

	private String portletId;
	private String title;
	private RenderMode renderMode;
	private Set<RenderMode> supportedModes;

	/**
	 * @param channels
	 * @param portletId
	 * @param mode
	 * @param supportedModes
	 */
	public RenderPortlet(String portletId, String title, RenderMode mode,
			Set<RenderMode> supportedModes) {
		super();
		this.portletId = portletId;
		this.title = title;
		this.renderMode = mode;
		this.supportedModes = supportedModes;
	}

	/**
	 * @return the portletId
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * @return the title
	 */
	public String title() {
		return title;
	}

	/**
	 * @return the renderMode
	 */
	public RenderMode renderMode() {
		return renderMode;
	}

	/**
	 * @return the supportedModes
	 */
	public Set<RenderMode> supportedRenderModes() {
		return supportedModes;
	}
}
