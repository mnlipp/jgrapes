/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.util.events.InitialConfiguration;

/**
 * A base class for configuration stores.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public abstract class ConfigurationStore extends Component {

    public static final Pattern NUMBER = Pattern.compile("^\\d+$");

    /**
     * Creates a new component with its channel set to itself.
     */
    public ConfigurationStore() {
        // Nothing to do.
    }

    /**
     * Creates a new component base with its channel set to the given 
     * channel. As a special case {@link Channel#SELF} can be
     * passed to the constructor to make the component use itself
     * as channel. The special value is necessary as you 
     * obviously cannot pass an object to be constructed to its 
     * constructor.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     */
    public ConfigurationStore(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Creates a new component base like {@link #ConfigurationStore(Channel)}
     * but with channel mappings for {@link Handler} annotations.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param channelReplacements the channel replacements to apply
     * to the `channels` elements of the {@link Handler} annotations
     */
    public ConfigurationStore(Channel componentChannel,
            ChannelReplacements channelReplacements) {
        super(componentChannel, channelReplacements);
    }

    /**
     * Each value in the map passed as argument represents properties
     * as a map of keys and (possibly) structured values (value, 
     * collection or map). Each such map of properties is converted
     * to a (possibly larger) map of properties where the structural
     * information has been added to the keys.
     *
     * @param structured the map with structured properties
     * @return the map with flattened properties
     */
    public static Map<String, String> flatten(Map<String, ?> structured) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> result = new HashMap<>();
        flattenObject(result, null, structured);
        return result;
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    private static void flattenObject(Map<String, String> result,
            String prefix, Object value) {
        if (value == null) {
            result.put(prefix, null);
            return;
        }
        if (value instanceof Map) {
            for (var entry : ((Map<String, ?>) value).entrySet()) {
                flattenObject(result,
                    Optional.ofNullable(prefix).map(p -> p + ".").orElse("")
                        + entry.getKey(),
                    entry.getValue());
            }
            return;
        }
        if (value instanceof Collection) {
            int count = 0;
            for (var item : (Collection<?>) value) {
                flattenObject(result, prefix + "." + count++, item);
            }
            return;
        }
        result.put(prefix, value.toString());
    }

    /**
     * Structure the given properties by looking for patterns in the keys.
     *
     * @param flatProperties the flat properties
     * @return the map
     */
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.ReturnEmptyCollectionRatherThanNull" })
    public static Map<String, Object>
            structure(Map<String, String> flatProperties) {
        if (flatProperties == null) {
            return null;
        }
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<Object, Object> result = new HashMap<>();
        for (var entry : flatProperties.entrySet()) {
            @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
            Map<Object, Object> target = result;
            // Original key (optionally) consists of dot separated parts
            StringTokenizer parts = new StringTokenizer(entry.getKey(), ".");
            while (true) {
                var part = parts.nextToken();
                Object key = NUMBER.matcher(part).find()
                    ? Integer.parseInt(part)
                    : part;
                if (!parts.hasMoreTokens()) {
                    // Last part (of key), store value
                    target.put(key, entry.getValue());
                    break;
                }
                @SuppressWarnings("unchecked")
                var newTarget = (Map<Object, Object>) target
                    .computeIfAbsent(part, k -> new TreeMap<Object, Object>());
                target = newTarget;
            }
        }

        // Now convert all maps that have only Integer keys to lists
        for (var entry : result.entrySet()) {
            entry.setValue(maybeConvert(entry.getValue()));
        }

        // Return result
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, Object> res = (Map<String, Object>) (Map) result;
        return res;
    }

    @SuppressWarnings({ "unchecked", "PMD.ConfusingTernary" })
    private static Object maybeConvert(Object value) {
        if (!(value instanceof Map)) {
            return value;
        }
        List<Object> converted = new ArrayList<>();
        for (var entry : ((Map<Object, Object>) value).entrySet()) {
            entry.setValue(maybeConvert(entry.getValue()));
            if (converted != null && entry.getKey() instanceof Integer) {
                converted.add(entry.getValue());
                continue;
            }
            converted = null;
        }
        return converted != null ? converted : value;
    }

    /**
     * Return the properties for a given path if they exists. This
     * method should only be used in cases where configuration values
     * are needed before the {@link InitialConfiguration} event is
     * fired, e.g. while creating the component tree. 
     * 
     * @param path the path
     * @return the values, if defined for the given path
     */
    public abstract Optional<Map<String, String>> values(String path);

    /**
     * Return the properties for a given path if they exists
     * as structured data, see {@link #values(String)}. 
     * 
     * @param path the path
     * @return the values, if defined for the given path
     */
    public Optional<Map<String, Object>> structured(String path) {
        return values(path).map(ConfigurationStore::structure);
    }

}
