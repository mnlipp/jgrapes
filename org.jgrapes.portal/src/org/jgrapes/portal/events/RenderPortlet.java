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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jgrapes.core.Event;

import static org.jgrapes.portal.Portlet.*;

/**
 * Send to the portal page for adding or updating a complete portlet
 * representation.
 */
public class RenderPortlet extends Event<Void> {

	private final static Set<RenderMode> DEFAULT_SUPPORTED 
		= Collections.unmodifiableSet(new HashSet<>(
				Arrays.asList(new RenderMode[] { RenderMode.Preview })));

	private Class<?> portletClass;
	private String portletId;
	private RenderMode renderMode = RenderMode.Preview;
	private Set<RenderMode> supportedModes = DEFAULT_SUPPORTED;
	private boolean foreground;
	private Reader contentReader;

	/**
	 * Creates a new event.
	 * 
	 * @param portletClass the portlet class
	 * @param portletId the id of the portlet
	 */
	public RenderPortlet(Class<?> portletClass, String portletId, 
			Reader contentReader) {
		this.portletClass = portletClass;
		this.portletId = portletId;
		this.contentReader = contentReader;
	}

	/**
	 * Set the render mode. The default value is {@link RenderMode#Preview}.
	 * 
	 * @param renderMode the render mode to set
	 * @return the event for easy chaining
	 */
	public RenderPortlet setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
		return this;
	}
	
	/**
	 * Set the supported render modes. The default value is 
	 * {@link RenderMode#Preview}.
	 * 
	 * @param supportedModes the supported render modes to set
	 * @return the event for easy chaining
	 */
	public RenderPortlet setSupportedModes(Set<RenderMode> supportedModes) {
		this.supportedModes = supportedModes;
		return this;
	}
	
	/**
	 * Add the given render mode to the supported render modes.
	 * 
	 * @param supportedMode the supported render modes to add
	 * @return the event for easy chaining
	 */
	public RenderPortlet addSupportedMode(RenderMode supportedMode) {
		if (supportedModes == DEFAULT_SUPPORTED) {
			supportedModes = new HashSet<>(DEFAULT_SUPPORTED);
		}
		supportedModes.add(supportedMode);
		return this;
	}
	
	/**
	 * Id set, the tab with the portlet is put in the foreground
	 * when the portlet is rendered. The default value is `false`.
	 * 
	 * @param foreground if set, the portlet is put in foreground
	 * @return the event for easy chaining
	 */
	public RenderPortlet setForeground(boolean foreground) {
		this.foreground = foreground;
		return this;
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
