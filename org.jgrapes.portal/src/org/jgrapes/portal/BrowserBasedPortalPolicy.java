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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.events.LastPortalLayout;
import org.jgrapes.portal.events.PortalLayoutChanged;
import org.jgrapes.portal.events.PortalPrepared;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * 
 */
public class BrowserBasedPortalPolicy extends Component {

	private static final String DATA_KEY 
		= "/" + BrowserBasedPortalPolicy.class.getName();

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
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws InterruptedException {
		Optional<PortalSession> session = lookupPortalSession(channel);
		if(session.isPresent()) {
				session.get().onPortalReady(event, channel);
		}
	}

	@Handler
	public void onPortalPrepared(PortalPrepared event, IOSubchannel channel) {
		lookupPortalSession(channel).ifPresent(
				ps -> ps.onPortalPrepared(event, channel));
	}

	@Handler
	public void onPortalLayoutChanged(PortalLayoutChanged event, 
			LinkedIOSubchannel channel) {
		lookupPortalSession(channel).ifPresent(
				ps -> ps.onPortalLayoutChanged(event, channel));
	}
	
	private Optional<PortalSession> lookupPortalSession(IOSubchannel channel) {
		return channel.associated(Session.class)
			.map(session -> (PortalSession)session.computeIfAbsent(
					BrowserBasedPortalPolicy.class, 
					k -> new PortalSession()));
	}
	
	private class PortalSession {

		private Map<String,Object> persisted = null;
		
		public void onPortalReady(PortalReady event, IOSubchannel channel) 
				throws InterruptedException {
			if (persisted != null) {
				return;
			}
			Map<String,String> data = newEventPipeline().fire(
					new KeyValueStoreQuery(DATA_KEY), channel).get();
			if (!data.containsKey(DATA_KEY)) {
				return;
			}
			try (ObjectInputStream in = new ObjectInputStream(
					Base64.getDecoder().wrap(
							new ByteArrayInputStream(
									data.get(DATA_KEY).getBytes("utf-8"))))) {
				@SuppressWarnings("unchecked")
				Map<String,Object> restored = (Map<String,Object>)in.readObject();
				persisted = restored;
			} catch (IOException | ClassNotFoundException e) {
				// cannot happen
			}
		}
		
		public void onPortalPrepared(
				PortalPrepared event, IOSubchannel channel) {
			if (persisted == null) {
				// Retrieval was not successful
				persisted = new HashMap<>();
			}
			// Make sure data is consistent
			String[][] previewLayout = (String[][])persisted.computeIfAbsent(
					"previewLayout", k -> { return new String[0][0]; });
			String[] tabsLayout = (String[])persisted.computeIfAbsent(
					"tabsLayout", k -> { return new String[0]; });

			// Update layout
			channel.respond(new LastPortalLayout(previewLayout, tabsLayout));
			
			// Restore portlets
			for (String portletId: tabsLayout) {
				fire(new RenderPortletRequest(
						event.event().renderSupport(), portletId,
						Portlet.RenderMode.View, false), channel);
			}
			for (String[] column: previewLayout) {
				for (String portletId: column) {
					fire(new RenderPortletRequest(
							event.event().renderSupport(), portletId,
							Portlet.RenderMode.Preview, true), channel);
				}
			}
		}
		
		public void onPortalLayoutChanged(
				PortalLayoutChanged event, IOSubchannel channel) {
			persisted.put("previewLayout", event.previewLayout());
			persisted.put("tabsLayout", event.tabsLayout());
			storeState(channel);
		}
		
		private void storeState(IOSubchannel channel) {
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(
					Base64.getEncoder().wrap(encoded))) {
				out.writeObject(persisted);
			} catch (IOException e) {
				// cannot happen
			}
			try {
				String data = encoded.toString("utf-8");
				fire(new KeyValueStoreUpdate()
						.update(DATA_KEY, data), channel);
			} catch (UnsupportedEncodingException e) {
				// cannot happen
			}
		}

	}
}
