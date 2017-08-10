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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;

import org.jdrupes.json.JsonBeanEncoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.events.JsonInput;
import org.jgrapes.portal.events.JsonOutput;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;
import org.jgrapes.util.events.KeyValueStoreUpdate.Action;
import org.jgrapes.util.events.KeyValueStoreUpdate.Deletion;
import org.jgrapes.util.events.KeyValueStoreUpdate.Update;

/**
 * 
 */
public class PortalLocalBackedKVStore extends Component {

	private String portalPrefix;
	boolean retrieved = false;
	private Map<String,String> data = new HashMap<>();
	
	/**
	 * Create a new key/value store that uses the browser's local storage
	 * for persisting the values.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 * @param portalPrefix the portal's prefix as returned by
	 * {@link Portal#prefix()}, i.e. staring and ending with a slash
	 */
	public PortalLocalBackedKVStore(
			Channel componentChannel, String portalPrefix) {
		super(componentChannel);
		this.portalPrefix = portalPrefix;
	}

	@Handler(priority=1000)
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws InterruptedException {
		if (retrieved) {
			return;
		}
		// Remove portal ready event from queue and save it
		event.cancel(false);
		channel.setAssociated(PortalLocalBackedKVStore.class, event);
		String keyStart = portalPrefix 
				+ PortalLocalBackedKVStore.class.getName();
		fire(new JsonOutput("retrieveLocalData", keyStart), channel);
	}
	
	@Handler
	public void onJsonInput(JsonInput event, LinkedIOSubchannel channel) 
			throws InterruptedException, IOException {
		if (!event.method().equals("retrievedLocalData")) {
			return;
		}
		channel.associated(PortalLocalBackedKVStore.class, PortalReady.class)
			.ifPresent(origEvent -> {
				JsonArray params = (JsonArray)event.params();
				data.clear();
				params.getJsonArray(0).forEach(item -> {
					JsonArray kvPair = (JsonArray)item;
					data.put(kvPair.getString(0), kvPair.getString(1));
				});
				retrieved = true;
				channel.setAssociated(PortalLocalBackedKVStore.class, null);
				// Let others process the portal ready event
				fire(new PortalReady(origEvent.renderSupport()), channel);
			});
	}
		
	@Handler
	public void onKeyValueStoreUpdate(
			KeyValueStoreUpdate event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		List<String[]> actions = new ArrayList<>();
		String keyStart = portalPrefix 
				+ PortalLocalBackedKVStore.class.getName();
		for (Action action: event.actions()) {
			String key = keyStart + (action.key().startsWith("/")
					? action.key() : ("/" + action.key()));
			if (action instanceof Update) {
				actions.add(new String [] {"u", key, 
						((Update)action).value() });
				data.put(action.key(), ((Update)action).value());
			} else if (action instanceof Deletion) {
				actions.add(new String [] {"d", key });
				data.remove(action.key());
			}
		}
		actions.add(new String[] { "u", keyStart, 
				JsonBeanEncoder.create().writeArray(
						data.keySet().toArray()).toJson() });
		fire(new JsonOutput("storeLocalData", 
				new Object[] { actions.toArray() }), channel);
	}

	@Handler
	public void onKeyValueStoreQuery(KeyValueStoreQuery event) {
		Map<String,String> result = new HashMap<>();
		if (!event.query().endsWith("/")) {
			// Single value
			if (data.containsKey(event.query())) {
				result.put(event.query(), data.get(event.query()));
			}
		} else {
			for (Map.Entry<String, String> e: data.entrySet()) {
				if (e.getKey().startsWith(event.query())) {
					result.put(e.getKey(), e.getValue());
				}
			}
		}
		event.setResult(result);
	}
}
