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

package org.jgrapes.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.prefs.BackingStoreException;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * This component provides a store for an application's configuration
 * backed by a JSON file. The JSON object described by this file 
 * represents the root directory. If an entry does not start with a
 * slash, it represents the key of a key value pair. If it does
 * starts with a slash, the value is another JSON object that
 * describes the respective subdirectory.
 * 
 * The component reads the initial values from {@link File} passed
 * to the constructor. During application bootstrap, it 
 * intercepts the {@link Start} event using a handler with  priority 
 * 999999. When receiving this event, it fires all known configuration 
 * values on the channels of the start event as a 
 * {@link ConfigurationUpdate} event. Then, it re-fires the 
 * intercepted {@link Start} event. 
 * 
 * Components that depend on configuration values define handlers
 * for {@link ConfigurationUpdate} events and adapt themselves to the values 
 * received. Note that due to the intercepted {@link Start} event, the initial
 * preferences values are received before the {@link Start} event, so
 * components' configurations can be rearranged before they actually
 * start doing something.
 *
 * Besides initially publishing the stored preferences values,
 * the component also listens for {@link ConfigurationUpdate} events
 * on its channel and updates the JSON file (may be suppressed).
 */
public class JsonConfigurationStore extends Component {

	private File file;
	private Map<String,Object> cache;
	private Set<Start> replacements = Collections.newSetFromMap(new WeakHashMap<>());
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel and the given file.
	 * 
	 * @param componentChannel the channel 
	 * @param file the file used to store the JSON
	 * @throws JsonDecodeException
	 */
	public JsonConfigurationStore(Channel componentChannel, File file)
		throws IOException {
		this(componentChannel, file, true);
	}

	/**
	 * Creates a new component with its channel set to the given 
	 * channel and the given file.
	 * 
	 * @param componentChannel the channel 
	 * @param file the file used to store the JSON
	 * @throws JsonDecodeException
	 */
	public JsonConfigurationStore(Channel componentChannel, File file, 
			boolean update) throws IOException {
		super(componentChannel);
		if (update) {
			Handler.Evaluator.add(this, "onConfigurationUpdate", 
					channel().defaultCriterion());
		}
		this.file = file;
		if (!file.exists()) {
			cache = new HashMap<>();
		}
		try (Reader in = new InputStreamReader(
				new FileInputStream(file), "utf-8")) {
			cache = (Map<String,Object>)JsonBeanDecoder
					.create(in).readObject();
		} catch (JsonDecodeException e) {
			throw new IOException(e);
		}
	}

	@Handler(priority=999999, channels=Channel.class)
	public void onStart(Start event) throws BackingStoreException {
		if (replacements.contains(event)) {
			return;
		}
		event.cancel(false);
		ConfigurationUpdate updEvt = new ConfigurationUpdate();
		addPrefs(updEvt, "/", cache);
		fire(updEvt, event.channels());
		Start replacement = new Start();
		replacements.add(replacement);
		fire(replacement, event.channels());
	}

	private void addPrefs(
			ConfigurationUpdate updEvt, String path, Map<String,?> map) {
		for (Map.Entry<String,?> e: map.entrySet()) {
			if (e.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String,?> value = (Map<String,?>)e.getValue();
				addPrefs(updEvt, ("/".equals(path) ? "" : path)
						+ e.getKey(), value);
				continue;
			}
			updEvt.add(path, e.getKey(), e.getValue().toString());
		}
	}
	
	@Handler(dynamic=true)
	public void onConfigurationUpdate(ConfigurationUpdate event) 
			throws IOException {
		boolean changed = false;
		for (String path: event.paths()) {
			if ("/".equals(path) && !event.values(path).isPresent()) {
				// Special case, "remove root", i.e. all configuration data
				cache.clear();
				changed = true;
			}
			changed = changed || handleSegment(cache, 
					new StringTokenizer(path, "/"), event.values(path));
		}
		if (changed) {
			try (Writer out = new OutputStreamWriter(
					new FileOutputStream(file), "utf-8")) {
				JsonBeanEncoder.create(out).writeObject(cache);
			}
		}
	}
	
	private boolean handleSegment(Map<String,Object> currentMap,
			StringTokenizer st,	Optional<Map<String,String>> values) {
		boolean changed = false;
		if (!st.hasMoreTokens()) {
			// "Leave" map
			return mergeValues(currentMap, values.get());
		}
		String nextSegment = "/" + st.nextToken();
		if (!st.hasMoreTokens() && !values.isPresent()) {
			// Next segment is last segment from path and we must remove
			if (currentMap.containsKey(nextSegment)) {
				// Delete sub-map.
				currentMap.remove(nextSegment);
				changed = true;
			}
			return changed;
		}
		// Check if next map exists
		@SuppressWarnings("unchecked")
		Map<String,Object> nextMap 
			= (Map<String,Object>)currentMap.get(nextSegment);
		if(nextMap == null) {
			// Doesn't exist, new sub-map
			changed = true;
			nextMap = new HashMap<>();
			currentMap.put(nextSegment, nextMap);
		}
		// Continue with sub-map
		return changed || handleSegment(nextMap, st, values);
	}

	private boolean mergeValues(Map<String, Object> currentMap,
	        Map<String, String> values) {
		boolean changed = false;
		for (Map.Entry<String,String> e: values.entrySet()) {
			if (e.getValue() == null) {
				// Delete from map
				if (currentMap.containsKey(e.getKey())) {
					currentMap.remove(e.getKey());
					changed = true;
				}
				continue;
			}
			String oldValue = Optional.ofNullable(currentMap.get(e.getKey()))
					.map(v -> v.toString()).orElse(null);
			if (oldValue == null || !e.getValue().equals(oldValue)) {
				currentMap.put(e.getKey(), e.getValue());
				changed = true;
			}
		}
		return changed;
	}
}
