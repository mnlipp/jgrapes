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

package org.jgrapes.http.demo.httpserver;

import java.util.Collections;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.Portlet;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.PortalConfigured;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portlets.markdowndisplay.MarkdownDisplayPortlet;
import org.jgrapes.portlets.markdowndisplay.MarkdownDisplayPortlet.Preferences;

/**
 * 
 */
public class NewPortalSessionPolicy extends Component {

	private final String renderedFlagName = getClass().getName() + ".rendered";
	
	/**
	 * Creates a new component with its channel set to
	 * itself.
	 */
	public NewPortalSessionPolicy() {
	}

	/**
	 * Creates a new component with its channel set to the given channel.
	 * 
	 * @param componentChannel
	 */
	public NewPortalSessionPolicy(Channel componentChannel) {
		super(componentChannel);
	}

	@Handler
	public void onPortalReady(PortalReady event, PortalSession portalsession) {
		portalsession.browserSession().put(renderedFlagName, false);
	}
	
	@Handler
	public void onRenderPortlet(RenderPortlet event, PortalSession portalsession) {
		portalsession.browserSession().put(renderedFlagName, true);
	}
	
	@Handler
	public void onPortalConfigured(PortalConfigured event, PortalSession portalSession) 
			throws InterruptedException {
		if ((Boolean)portalSession.browserSession().getOrDefault(
					renderedFlagName, false)) {
			return;
		}
		fire(new AddPortletRequest(event.event().event().renderSupport(), 
				MarkdownDisplayPortlet.class.getName(),
				Portlet.RenderMode.Preview)
				.addOption(Preferences.TITLE, "Demo Portal")
				.addOption(Preferences.PREVIEW_SOURCE, "A Demo Portal")
				.addOption(Preferences.EDITABLE_BY,  Collections.EMPTY_SET),
				portalSession);
	}

}
