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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.util.ConfigurationStore;

/**
 * An event to indicate that configuration information has been
 * updated.
 * 
 * Configuration information provided by this event is organized
 * by paths and associated key/value pairs. The path information
 * should be used by components to select the information important
 * to them. Often, a component simply matches the path from the event
 * with its own path in the component hierarchy 
 * (see {@link Manager#componentPath()}). But paths can also be used
 * to structure information in a way that is completely independent of
 * the implementation's structure as the filtering is completely up
 * to the component.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ConfigurationUpdate extends Event<Void> {

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Map<String, Object>> structuredValues
        = new HashMap<>();
    private final Map<String, Map<String, Object>> flattenedCache
        = new ConcurrentHashMap<>();

    /**
     * Return all paths affected by this event.
     * 
     * @return the paths
     */
    @SuppressWarnings("PMD.ConfusingTernary")
    public Set<String> paths() {
        synchronized (structuredValues) {
            return new HashSet<>(structuredValues.keySet());
        }
    }

    private Optional<Map<String, Object>> flattened(String path) {
        return Optional.ofNullable(flattenedCache.computeIfAbsent(path,
            p -> ConfigurationStore.flatten(structuredValues.get(path))));
    }

    /**
     * Return the properties for a given path if any exist.
     * If a property has a structured value (list or collection),
     * the values are returned as several entries as described in
     * {@link ConfigurationStore#flatten(Map)}. All values are
     * converted to their string representation.
     * 
     * @param path the path
     * @return the updated values or `null` if the path has been
     * removed (implies the removal of all values for that path).
     */
    public Optional<Map<String, String>> values(String path) {
        if (structuredValues.get(path) == null) {
            return Optional.empty();
        }
        Map<String, Object> result = flattened(path).get();
        return Optional.of(result).map(o -> o.entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e -> ConfigurationStore
                    .as(e.getValue(), String.class).orElse(null))));
    }

    /**
     * Return the value with the given path and key if it exists
     * and is of or can be converted to the requested type.
     *
     * @param <T> the generic type
     * @param path the path
     * @param key the key
     * @param as the as
     * @return the optional
     */
    @SuppressWarnings("PMD.ShortVariable")
    public <T> Optional<T> value(String path, String key, Class<T> as) {
        return flattened(path)
            .flatMap(map -> ConfigurationStore.as(map.get(key), as));
    }

    /**
     * Return the value with the given path and key if it exists as string.
     * 
     * @param path the path
     * @param key the key
     * @return the value
     */
    public Optional<String> value(String path, String key) {
        return value(path, key, String.class);
    }

    /**
     * Return the properties for a given path if they exists as
     * a map with (possibly) structured values (see 
     * {@link ConfigurationStore#structured(String)}). The type
     * of the value depends on the configuration store used.
     * Some configuration stores support types other than string,
     * some don't. Too avoid any problems, it is strongly recommended
     * to call {@link ConfigurationStore#as(Object, Class)} for any
     * value obtained from the result of this method.
     * 
     * @param path the path
     * @return the updated values or `null` if the path has been
     * removed (implies the removal of all values for that path).
     */
    public Optional<Map<String, Object>> structured(String path) {
        if (structuredValues.get(path) == null) {
            return Optional.empty();
        }
        return Optional
            .of(Collections.unmodifiableMap(structuredValues.get(path)));
    }

    /**
     * Set new (updated), possibly structured configuration values (see
     * {@link ConfigurationStore#structure(Map)} for the given path.
     * Any information associated with the path before the invocation
     * of this method is replaced.  
     * 
     * @param path the value's path
     * @return the event for easy chaining
     */
    @SuppressWarnings("unchecked")
    public ConfigurationUpdate set(String path, Map<String, ?> values) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with \"/\".");
        }
        structuredValues.put(path, (Map<String, Object>) values);
        return this;
    }

    /**
     * Add a new (or updated) configuration value for the given path
     * and key.
     * 
     * @param path the value's path
     * @param selector the key or the path within the structured value
     * @param value the value
     * @return the event for easy chaining
     */
    public ConfigurationUpdate add(String path, String selector, Object value) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with \"/\".");
        }
        synchronized (structuredValues) {
            @SuppressWarnings("PMD.UseConcurrentHashMap")
            Map<String, Object> scoped = structuredValues
                .computeIfAbsent(path, newKey -> new HashMap<String, Object>());
            ConfigurationStore.mergeValue(scoped, selector, value);
            flattenedCache.remove(path);
        }
        return this;
    }

    /**
     * Associate the given path with `null`. This signals to handlers 
     * that the path has been removed from the configuration.
     * 
     * @param path the path that has been removed
     * @return the event for easy chaining
     */
    public ConfigurationUpdate removePath(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with \"/\".");
        }
        structuredValues.put(path, null);
        return this;
    }

}
