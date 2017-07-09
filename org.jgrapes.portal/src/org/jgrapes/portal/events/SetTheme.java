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
import org.jgrapes.portal.PortalView;

/**
 * Signals that the theme for the portal has changed. This  event is
 * handled by the {@link PortalView} but may, of course, also be
 * used by other components.
 * 
 * ![Event Sequence](SetTheme.svg)
 * 
 * @startuml SetTheme.svg
 * hide footbox
 * 
 * Browser -> Portal: "settheme"
 * activate Portal
 * Portal -> PortalView: SetTheme
 * deactivate Portal
 * activate PortalView
 * PortalView -> Browser: "reload"
 * deactivate PortalView
 * 
 * @enduml
 */
public class SetTheme extends Event<Void> {

	private String theme;

	/**
	 * Creates a new event.
	 * 
	 * @param theme the theme to set
	 */
	public SetTheme(String theme) {
		this.theme = theme;
	}

	/**
	 * Returns the theme to set.
	 * 
	 * @return the theme
	 */
	public String theme() {
		return theme;
	}
}
