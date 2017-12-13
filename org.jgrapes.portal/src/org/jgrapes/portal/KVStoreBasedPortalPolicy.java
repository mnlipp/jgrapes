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
import org.jgrapes.portal.events.LastPortalLayout;
import org.jgrapes.portal.events.PortalLayoutChanged;
import org.jgrapes.portal.events.PortalPrepared;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

/**
 * A component that restores the portal layout,
 * using key/value events for persisting the data between sessions.
 * 
 * ![Boot Event Sequence](KVPPBootSeq.svg)
 * 
 * This component requires another component that handles the key/value
 * store events ({@link KeyValueStoreUpdate}, {@link KeyValueStoreQuery})
 * used by this component for implementing persistence. When the portal becomes
 * ready, this policy sends a query for the persisted data. To ensure
 * that the data is available before the boot sequence continues, the 
 * completion of the {@link PortalReady} event is delayed (using a 
 * {@link CompletionLock}) until the requested data becomes available.
 * 
 * When the portal has been prepared, the policy sends the last layout
 * as retrieved from persistent storage to the portal and then generates
 * render events for all portlets contained in this layout.
 * 
 * Each time the layout is changed in the portal, the portal sends the
 * new layout data and this component updates the persistent storage
 * accordingly.
 * 
 * @startuml KVPPBootSeq.svg
 * hide footbox
 * 
 * Browser -> Portal: "portalReady"
 * activate Portal
 * Portal -> KVStoreBasedPortalPolicy: PortalReady
 * deactivate Portal
 * activate KVStoreBasedPortalPolicy
 * KVStoreBasedPortalPolicy -> "KV Store": KeyValueStoreQuery
 * activate "KV Store"
 * "KV Store" -> KVStoreBasedPortalPolicy: KeyValueStoreData
 * deactivate "KV Store"
 * deactivate KVStoreBasedPortalPolicy
 * 
 * actor System
 * System -> KVStoreBasedPortalPolicy: PortalPrepared
 * activate KVStoreBasedPortalPolicy
 * KVStoreBasedPortalPolicy -> Portal: LastPortalLayout
 * activate Portal
 * Portal -> Browser: "lastPortalLayout"
 * deactivate Portal
 * loop for all portlets to be displayed
 *     KVStoreBasedPortalPolicy -> PortletX: RenderPortletRequest
 *     activate PortletX
 *     PortletX -> Portal: RenderPortlet
 *     deactivate PortletX
 *     activate Portal
 *     Portal -> Browser: "renderPortlet"
 *     deactivate Portal
 * end
 * deactivate KVStoreBasedPortalPolicy
 * 
 * Browser -> Portal: "portalLayout"
 * activate Portal
 * Portal -> KVStoreBasedPortalPolicy: PortalLayoutChanged
 * deactivate Portal
 * activate KVStoreBasedPortalPolicy
 * KVStoreBasedPortalPolicy -> "KV Store": KeyValueStoreUpdate
 * deactivate KVStoreBasedPortalPolicy
 * 
 * @enduml
 */
public class KVStoreBasedPortalPolicy extends Component {

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
	 * session data from the key/value store and resume.
	 * 
	 * @param event
	 * @param channel
	 * @throws InterruptedException
	 */
	@Handler
	public void onPortalReady(PortalReady event, PortalSession channel) 
			throws InterruptedException {
		PortalSessionDataStore sessionDs = channel.associated(
				PortalSessionDataStore.class, 
				() -> new PortalSessionDataStore(channel.browserSession()));
		sessionDs.onPortalReady(event, channel);
	}

	@Handler
	public void onKeyValueStoreData(
			KeyValueStoreData event, PortalSession channel) 
					throws JsonDecodeException {
		Optional<PortalSessionDataStore> optSessionDs 
			= channel.associated(PortalSessionDataStore.class);
		if (optSessionDs.isPresent()) {
			optSessionDs.get().onKeyValueStoreData(event, channel);
		}
	}

	@Handler
	public void onPortalPrepared(
			PortalPrepared event, PortalSession channel) {
		channel.associated(PortalSessionDataStore.class).ifPresent(
				ps -> ps.onPortalPrepared(event, channel));
	}

	@Handler
	public void onPortalLayoutChanged(PortalLayoutChanged event, 
			PortalSession channel) {
		channel.associated(PortalSessionDataStore.class).ifPresent(
				ps -> ps.onPortalLayoutChanged(event, channel));
	}
	
	private class PortalSessionDataStore {

		private String storagePath;
		private CompletionLock readyLock;
		private Map<String,Object> persisted = null;
		
		public PortalSessionDataStore(Session session) {
			storagePath = "/" 
					+ Utils.userFromSession(session)
					.map(UserPrincipal::toString).orElse("")
					+ "/" + KVStoreBasedPortalPolicy.class.getName();
		}
		
		public void onPortalReady(PortalReady event, IOSubchannel channel) 
				throws InterruptedException {
			if (persisted != null) {
				return;
			}
			KeyValueStoreQuery query = new KeyValueStoreQuery(
					storagePath, true);
			readyLock = new CompletionLock(event, 3000);
			fire(query, channel);
		}

		public void onKeyValueStoreData(
				KeyValueStoreData event, IOSubchannel channel) 
						throws JsonDecodeException {
			if (!event.event().query().equals(storagePath)) {
				return;
			}
			String data = event.data().get(storagePath);
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
			
			// Now store.
			JsonBeanEncoder encoder = JsonBeanEncoder.create();
			encoder.writeObject(persisted);
			fire(new KeyValueStoreUpdate()
					.update(storagePath, encoder.toJson()), channel);
		}

	}
}
