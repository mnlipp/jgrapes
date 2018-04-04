/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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
import java.util.Optional;
import java.util.Set;

import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;

/**
 * An event to indicate that configuration information has been
 * updated.
 * 
 * Configurability based on this type of event can optionally be 
 * supported by components. If supported, a component implements a 
 * handler that checks whether the paths of this event include the 
 * component's path in the application's component tree (see 
 * {@link Manager#componentPath()}). If so, the component adapts itself to the 
 * information propagated.
 */
public class ConfigurationUpdate extends Event<Void> {

	@SuppressWarnings("PMD.UseConcurrentHashMap")
	private final Map<String,Map<String,String>> paths = new HashMap<>();

	/**
	 * Add new (updated) configuration value to the event.
	 * 
	 * @param path the value's path
	 * @param key the key of the value
	 * @param value the value
	 * @return the event for easy chaining
	 */
	public ConfigurationUpdate add(String path, String key, String value) {
		if (path == null || !path.startsWith("/")) {
			throw new IllegalArgumentException("Path must start with \"/\".");
		}
		@SuppressWarnings("PMD.UseConcurrentHashMap")
		Map<String,String> scoped = paths
				.computeIfAbsent(path, newKey -> new HashMap<String,String>());
		scoped.put(key, value);
		return this;
	}

	/**
	 * Remove a path from the configuration.
	 * 
	 * @param path the path to be removed
	 * @return the event for easy chaining
	 */
	public ConfigurationUpdate removePath(String path) {
		if (path == null || !path.startsWith("/")) {
			throw new IllegalArgumentException("Path must start with \"/\".");
		}
		paths.put(path, null);
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
	 * Return the values for a given path if they exists.
	 * 
	 * @param path the path
	 * @return the updated values or `null` if the path has been
	 * removed (implies the removal of all values for that path).
	 */
	public Optional<Map<String,String>> values(String path) {
		Map<String,String> result = paths.get(path);
		if (result == null) {
			return Optional.empty();
		}
		return Optional.of(Collections.unmodifiableMap(result));
	}

	/**
	 * Return the value with the given path and key if it exists.
	 * 
	 * @param path the path
	 * @param key the key
	 * @return the value
	 */
	public Optional<String> value(String path, String key) {
		return Optional.ofNullable(paths.get(path))
				.flatMap(map -> Optional.ofNullable(map.get(key)));
	}
}
