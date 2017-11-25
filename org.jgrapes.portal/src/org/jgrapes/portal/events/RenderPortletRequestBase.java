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
import org.jgrapes.portal.Portlet.RenderMode;
import org.jgrapes.portal.RenderSupport;

/**
 * The base class for events that result in a portlet being rendered.
 */
public abstract class RenderPortletRequestBase<T> extends Event<T> {
	private RenderSupport renderSupport;
	private RenderMode renderMode;

	/**
	 * Creates a new event.
	 * 
	 * @param renderSupport the render support
	 * @param mode the view mode that is requested
	 */
	public RenderPortletRequestBase(
			RenderSupport renderSupport, RenderMode mode) {
		this.renderSupport = renderSupport;
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
	 * Returns the render mode.
	 * 
	 * @return the render mode
	 */
	public RenderMode renderMode() {
		return renderMode;
	}

}
