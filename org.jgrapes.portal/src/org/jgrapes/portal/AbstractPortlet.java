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

package org.jgrapes.portal;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;

/**
 * 
 */
public abstract class AbstractPortlet extends Component {	
	
	/**
	 * @param componentChannel
	 */
	public AbstractPortlet(Channel componentChannel) {
		super(componentChannel);
	}

	protected abstract String portletId();
	
	@Handler
	public void onResourceRequest(
			PortletResourceRequest event, IOSubchannel channel) {
		// For me?
		if (!event.portletType().equals(getClass().getName())) {
			return;
		}
		
		// Look for content
		InputStream in = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (in == null) {
			return;
		}

		// Respond
		channel.respond(new PortletResourceResponse(event, in));
		
		// Done
		event.setResult(true);
	}

	@SuppressWarnings("unchecked")
	protected Map<Object, Object> portletSession(IOSubchannel channel) {
		return channel.associated(Session.class).map(session ->
				(Map<Object,Object>)session.computeIfAbsent(portletId(),
						k -> new HashMap<>()))
				.orElseThrow(IllegalStateException::new);
	}
}
