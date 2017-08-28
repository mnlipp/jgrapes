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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionLock;
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
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * 
 */
public class KVStoreBasedPortalPolicy extends Component {

	private static final String DATA_KEY 
		= "/" + KVStoreBasedPortalPolicy.class.getName();

	/**
	 * Creates a new component with its channel set to
	 * itself.
	 */
	public KVStoreBasedPortalPolicy() {
	}

	/**
	 * Creates a new component with its channel set to the given channel.
	 * 
	 * @param componentChannel
	 */
	public KVStoreBasedPortalPolicy(Channel componentChannel) {
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
	public void onKeyValueStoreData(
			KeyValueStoreData event, IOSubchannel channel) 
					throws JsonDecodeException {
		Optional<PortalSession> optSession = lookupPortalSession(channel);
		if (optSession.isPresent()) {
			optSession.get().onKeyValueStoreData(event, channel);
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
					KVStoreBasedPortalPolicy.class, 
					k -> new PortalSession()));
	}
	
	private class PortalSession {

		private CompletionLock readyLock;
		private Map<String,Object> persisted = null;
		
		public void onPortalReady(PortalReady event, IOSubchannel channel) 
				throws InterruptedException {
			if (persisted != null) {
				return;
			}
			KeyValueStoreQuery query = new KeyValueStoreQuery(DATA_KEY, true);
			readyLock = new CompletionLock(event, 3000);
			fire(query, channel);
		}

		public void onKeyValueStoreData(
				KeyValueStoreData event, IOSubchannel channel) 
						throws JsonDecodeException {
			if (!event.event().query().equals(DATA_KEY)) {
				return;
			}
			String data = event.data().get(DATA_KEY);
			if (data != null) {
				JsonBeanDecoder decoder = JsonBeanDecoder.create(data);
				@SuppressWarnings("unchecked")
				Class<Map<String,Object>> cls 
					= (Class<Map<String,Object>>)(Class<?>)HashMap.class;
				persisted = decoder.readObject(cls);
			}
			readyLock.remove();
			readyLock = null;
		}
		
		public void onPortalPrepared(
				PortalPrepared event, IOSubchannel channel) {
			if (persisted == null) {
				// Retrieval was not successful
				persisted = new HashMap<>();
			}
			// Make sure data is consistent
			@SuppressWarnings("unchecked")
			List<List<String>> previewLayout = (List<List<String>>)persisted
				.computeIfAbsent(
					"previewLayout", k -> { return Collections.emptyList(); });
			@SuppressWarnings("unchecked")
			List<String> tabsLayout = (List<String>)persisted.computeIfAbsent(
					"tabsLayout", k -> { return Collections.emptyList(); });

			// Update layout
			channel.respond(new LastPortalLayout(previewLayout, tabsLayout));
			
			// Restore portlets
			for (String portletId: tabsLayout) {
				fire(new RenderPortletRequest(
						event.event().renderSupport(), portletId,
						Portlet.RenderMode.View, false), channel);
			}
			for (List<String> column: previewLayout) {
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
			JsonBeanEncoder encoder = JsonBeanEncoder.create();
			encoder.writeObject(persisted);
			fire(new KeyValueStoreUpdate()
						.update(DATA_KEY, encoder.toJson()), channel);
		}

	}
}
