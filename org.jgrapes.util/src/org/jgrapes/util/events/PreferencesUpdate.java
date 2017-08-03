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

package org.jgrapes.util.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jgrapes.core.Event;

/**
 * An event to indicate that preferences values have been updated.
 */
public class PreferencesUpdate extends Event<Void> {

	private Map<String,Map<String,String>> paths = new HashMap<>();

	/**
	 * Add new (updated) preferences to the event.
	 * 
	 * @param path the preference's path
	 * @param key the key of the preference
	 * @param value the value of the preference
	 * @return the event for easy chaining
	 */
	public PreferencesUpdate add(String path, String key, String value) {
		if (path == null) {
			path = "";
		}
		Map<String,String> scoped = paths
				.computeIfAbsent(path, s -> new HashMap<String,String>());
		scoped.put(key, value);
		return this;
	}

	/**
	 * Return all paths affected by this event.
	 * 
	 * @return the paths
	 */
	public Set<String> paths() {
		return Collections.unmodifiableSet(paths.keySet());
	}
	
	/**
	 * Return the preferences for a given path.
	 * 
	 * @param path the path
	 * @return the updated preferences
	 */
	public Map<String,String> preferences(String path) {
		return Collections.unmodifiableMap(
				paths.getOrDefault(path, Collections.emptyMap()));
	}
}
