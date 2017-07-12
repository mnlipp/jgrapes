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

import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.http.demo.portlets.helloworld.HelloWorldPortlet;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.Portlet;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.PortalPrepared;

/**
 * 
 */
public class NewPortalSessionPolicy extends Component {

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
	public void onPortalPrepared(PortalPrepared event, IOSubchannel channel) 
			throws InterruptedException {
		Optional<Session> optSession = channel.associated(Session.class);
		if (optSession.isPresent()) {
			final String flagName = getClass().getName() + ".processed";
			if ((Boolean)optSession.get().getOrDefault(flagName, false)) {
				return;
			}
			fire(new AddPortletRequest(event.event().renderSupport(), 
					HelloWorldPortlet.class.getName(), 
					Portlet.RenderMode.DeleteablePreview), channel);
			optSession.get().put(flagName, true);
		};
	}

}
