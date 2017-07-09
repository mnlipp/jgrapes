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

import java.util.Locale;

import org.jgrapes.core.Event;
import org.jgrapes.portal.PortalView;

/**
 * Signals that the locale for the portal has changed. This  event is
 * handled by the {@link PortalView} but may, of course, also be
 * used by other components.
 * 
 * ![Event Sequence](SetLocale.svg)
 * 
 * @startuml SetLocale.svg
 * hide footbox
 * 
 * Browser -> Portal: "setLocale"
 * activate Portal
 * Portal -> PortalView: SetLocale
 * deactivate Portal
 * activate PortalView
 * PortalView -> Browser: "reload"
 * deactivate PortalView
 * 
 * @enduml
 */
public class SetLocale extends Event<Void> {

	private Locale locale;

	/**
	 * Creates a new event.
	 * 
	 * @param locale the locale to set
	 */
	public SetLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Returns the locale to set.
	 * 
	 * @return the locale
	 */
	public Locale locale() {
		return locale;
	}
}
