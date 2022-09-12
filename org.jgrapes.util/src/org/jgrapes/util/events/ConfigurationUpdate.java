/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.util.events;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.util.ConfigurationStore;

/**
 * An event to indicate that configuration information has been
 * updated.
 * 
 * Configuration information provided by this event is organized
 * by paths and provided as key/value pairs. The path information
 * should be used by components to select the information important
 * to them. Often, a component simply matches the path from the event
 * with its own path in the component hierarchy 
 * (see {@link Manager#componentPath()}). But paths can also be used
 * to structure information in a way that is completely independent of
 * the implementation's structure as the filtering is completely up
 * to the component.
 * 
 * Configuration information should be kept simple. Sometimes, however,
 * it is unavoidable to structure the information associated
 * with a (logical) key. This should be done by reflecting the structure
 * in the names of the keys. Names "key.0", "key.1", "key.2"
 * can be used to express that the value associated with "key" is 
 * actually a list of values. "key.a", "key.b", "key.c" can be used
 * to associate "key" with a map from "a", "b", "c" to some values.
 * To support this kind of structuring by keys, the method 
 * {@link #set(String, Map)} can be used to associate a path with
 * structured information, consisting of (nested) {@link Map}s and 
 * {@link Collection}s. The leaf nodes in this structures are used
 * as values after applying {@link Object#toString()} to them.
 * Method {@link #structured(String)} can be used to retrieve information
 * as structured value, no matter if the information was initially provided
 * by {@link #set(String, Map)} or by several invocations of
 * {@link #add(String, String, String)}.
 * 
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ConfigurationUpdate extends Event<Void> {

    @SuppressWarnings({ "PMD.UseConcurrentHashMap",
        "PMD.AvoidDuplicateLiterals" })
    private final Map<String, Map<String, String>> flatValues = new HashMap<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Map<String, ?>> structuredValues
        = new HashMap<>();

    /**
     * Set new (updated) structured configuration values for the event.
     * Any information associated with the path before the invocation
     * of this method is replaced.  
     * 
     * @param path the value's path
     * @return the event for easy chaining
     */
    public ConfigurationUpdate set(String path, Map<String, ?> values) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with \"/\".");
        }
        synchronized (this) {
            flatValues.remove(path);
            structuredValues.put(path, values);
        }
        return this;
    }

    /**
     * Return the structured properties for a given path if they exists.
     * 
     * @param path the path
     * @return the updated values or `null` if the path has been
     * removed (implies the removal of all values for that path).
     */
    public Optional<Map<String, Object>> structured(String path) {
        synchronized (this) {
            Map<String, ?> result;
            if (!structuredValues.containsKey(path)
                && flatValues.containsKey(path)) {
                // Synchronize to structured and return
                result = ConfigurationStore.structure(flatValues.get(path));
                structuredValues.put(path, result);
            } else {
                result = structuredValues.get(path);
            }
            if (result == null) {
                return Optional.empty();
            }
            return Optional.of(Collections.unmodifiableMap(result));
        }
    }

    /**
     * Add a new (or updated) configuration value to the event.
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
        synchronized (this) {
            if (!flatValues.containsKey(path)
                && structuredValues.containsKey(path)) {
                // Flattened version becomes current, invalidate structured
                flatValues.put(path,
                    ConfigurationStore.flatten(structuredValues.get(path)));
            }
            structuredValues.remove(path);
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<String, String> scoped = flatValues
                .computeIfAbsent(path, newKey -> new HashMap<String, String>());
            scoped.put(key, value);
        }
        return this;
    }

    /**
     * Signal to handlers that a path has been removed from the 
     * configuration.
     * 
     * @param path the path that has been removed
     * @return the event for easy chaining
     */
    public ConfigurationUpdate removePath(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with \"/\".");
        }
        synchronized (this) {
            flatValues.put(path, null);
            structuredValues.remove(path);
        }
        return this;
    }

    /**
     * Return all paths affected by this event.
     * 
     * @return the paths
     */
    @SuppressWarnings("PMD.ConfusingTernary")
    public Set<String> paths() {
        synchronized (this) {
            Set<String> result = new HashSet<>(flatValues.keySet());
            result.addAll(structuredValues.keySet());
            return result;
        }
    }

    /**
     * Return the properties for a given path if they exists.
     * 
     * @param path the path
     * @return the updated values or `null` if the path has been
     * removed (implies the removal of all values for that path).
     */
    public Optional<Map<String, String>> values(String path) {
        synchronized (this) {
            Map<String, String> result;
            if (!flatValues.containsKey(path)
                && structuredValues.containsKey(path)) {
                // Synchronize to flatValues and return
                result = ConfigurationStore.flatten(structuredValues.get(path));
                flatValues.put(path, result);
            } else {
                result = flatValues.get(path);
            }
            if (result == null) {
                return Optional.empty();
            }
            return Optional.of(Collections.unmodifiableMap(result));
        }
    }

    /**
     * Return the value with the given path and key if it exists.
     * 
     * @param path the path
     * @param key the key
     * @return the value
     */
    public Optional<String> value(String path, String key) {
        if (!flatValues.containsKey(path)
            && structuredValues.containsKey(path)) {
            flatValues.put(path,
                ConfigurationStore.flatten(structuredValues.get(path)));
        }
        return Optional.ofNullable(flatValues.get(path))
            .flatMap(map -> Optional.ofNullable(map.get(key)));
    }
}
