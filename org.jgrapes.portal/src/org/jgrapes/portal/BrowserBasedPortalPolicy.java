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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonWriter;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.events.DataRetrieved;
import org.jgrapes.portal.events.PortalLayoutChanged;
import org.jgrapes.portal.events.PortalPrepared;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RetrieveDataFromPortal;
import org.jgrapes.portal.events.StoreDataInPortal;
import org.jgrapes.portal.util.JsonUtil;

/**
 * 
 */
public class BrowserBasedPortalPolicy extends Component {

	/**
	 * Creates a new component with its channel set to
	 * itself.
	 */
	public BrowserBasedPortalPolicy() {
	}

	/**
	 * Creates a new component with its channel set to the given channel.
	 * 
	 * @param componentChannel
	 */
	public BrowserBasedPortalPolicy(Channel componentChannel) {
		super(componentChannel);
	}

	/**
	 * Intercept the {@link PortalReady} event. Request the 
	 * session data from the portal and resume.
	 * 
	 * @param event
	 * @param channel
	 * @throws InterruptedException
	 */
	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) {
		lookupPortalSession(channel).ifPresent(ps -> ps.onPortalReady(event));
	}

	@Handler
	public void onDataRetrieved(DataRetrieved event, IOSubchannel channel) {
		if (!event.path().equals(BrowserBasedPortalPolicy.class.getName())) {
			return;
		}
		lookupPortalSession(channel).ifPresent(
				ps -> ps.onDataRetrieved(event));
	}

	@Handler
	public void onPortalPrepared(PortalPrepared event, IOSubchannel channel) {
		lookupPortalSession(channel).ifPresent(
				ps -> ps.onPortalPrepared(event));
	}

	@Handler
	public void onPortalLayoutChanged(PortalLayoutChanged event, 
			LinkedIOSubchannel channel) {
		lookupPortalSession(channel).ifPresent(
				ps -> ps.onPortalLayoutChanged(event));
	}
	
	private Optional<PortalSession> lookupPortalSession(IOSubchannel channel) {
		return channel.associated(Session.class)
			.map(session -> (PortalSession)session.computeIfAbsent(
					BrowserBasedPortalPolicy.class, 
					k -> new PortalSession(channel)));
	}
	
	private class PortalSession {

		private IOSubchannel channel;
		private RenderSupport pendingRenderSupport = null;
		private Map<String,Object> persisted = null;
		
		/**
		 * @param channel
		 */
		public PortalSession(IOSubchannel channel) {
			super();
			this.channel = channel;
		}

		private void storeState() {
			JsonArray jsonData = JsonUtil.toJsonArray(persisted);
			StringWriter out = new StringWriter(); 
			try (JsonWriter jsonWriter = Json.createWriter(out)) {
				jsonWriter.write(jsonData);
			}
			fire(new StoreDataInPortal(BrowserBasedPortalPolicy.class.getName(),
					out.toString()), channel);
		}

		public void onPortalReady(PortalReady event) {
			if (persisted != null) {
				return;
			}
			channel.respond(new RetrieveDataFromPortal(
					BrowserBasedPortalPolicy.class.getName()));
		}
		
		public void onDataRetrieved(DataRetrieved event) {
			persisted = new HashMap<>();
			if (pendingRenderSupport != null) {
				handlePrepared(pendingRenderSupport);
			}
			pendingRenderSupport = null;
		}
		
		public void onPortalPrepared(PortalPrepared event) {
			if (persisted == null) {
				pendingRenderSupport = event.event().renderSupport();
				return;
			}
			handlePrepared(event.event().renderSupport());
		}
		
		private void handlePrepared(RenderSupport renderSupport) {
			System.out.println("prepared");
		}
		
		public void onPortalLayoutChanged(PortalLayoutChanged event) {
			persisted.put("previewLayout", event.previewLayout());
			persisted.put("tabsLayout", event.tabsLayout());
			storeState();
		}
	}
}
