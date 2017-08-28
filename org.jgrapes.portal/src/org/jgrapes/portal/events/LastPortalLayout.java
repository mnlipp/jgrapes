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

import java.util.List;

import org.jgrapes.core.Event;

/**
 * Sent by the server to the browser in response to {@link PortalPrepared} 
 * (see this event's description for details). The provided information
 * enables the portal to restore portlets to their previous positions.
 */
public class LastPortalLayout extends Event<Void> {

	private List<List<String>> previewLayout;
	private List<String> tabsLayout;
	
	/**
	 * @param previewLayout
	 * @param tabsLayout
	 */
	public LastPortalLayout(
			List<List<String>> previewLayout, List<String> tabsLayout) {
		this.previewLayout = previewLayout;
		this.tabsLayout = tabsLayout;
	}

	/**
	 * @return the previewLayout
	 */
	public List<List<String>> previewLayout() {
		return previewLayout;
	}

	/**
	 * @return the tabsLayout
	 */
	public List<String> tabsLayout() {
		return tabsLayout;
	}

}
