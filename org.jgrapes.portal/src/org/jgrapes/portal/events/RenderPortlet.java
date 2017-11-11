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

import java.io.Reader;
import java.util.Set;
import org.jgrapes.core.Event;

import static org.jgrapes.portal.Portlet.*;

/**
 * Send to the portal page for adding or updating a complete portlet
 * representation.
 */
public class RenderPortlet extends Event<Void> {

	private Class<?> portletClass;
	private String portletId;
	private RenderMode renderMode;
	private Set<RenderMode> supportedModes;
	private boolean foreground;
	private Reader contentReader;

	/**
	 * Creates a new event.
	 * 
	 * @param portletClass the portlet class
	 * @param portletId the id of the portlet
	 * @param mode the view mode that is to be updated
	 * @param supportedModes the modes supported by the portlet
	 */
	public RenderPortlet(Class<?> portletClass, String portletId, 
			RenderMode mode, Set<RenderMode> supportedModes, 
			boolean foreground, Reader contentReader) {
		this.portletClass = portletClass;
		this.portletId = portletId;
		this.renderMode = mode;
		this.supportedModes = supportedModes;
		this.foreground = foreground;
		this.contentReader = contentReader;
	}

	public Class<?> portletClass() {
		return portletClass;
	}
	
	/**
	 * Returns the portlet id
	 * 
	 * @return the portlet id
	 */
	public String portletId() {
		return portletId;
	}

	/**
	 * Returns the render mode.
	 * 
	 * @return the render mode
	 */
	public RenderMode renderMode() {
		return renderMode;
	}

	/**
	 * Returns the supported modes.
	 * 
	 * @return the supported modes
	 */
	public Set<RenderMode> supportedRenderModes() {
		return supportedModes;
	}

	/**
	 * Indicates if portelt is to be put in foreground.
	 * 
	 * @return the result
	 */
	public boolean isForeground() {
		return foreground;
	}

	/**
	 * Returns the content reader.
	 * 
	 * @return the content reader
	 */
	public Reader contentReader() {
		return contentReader;
	}
}
