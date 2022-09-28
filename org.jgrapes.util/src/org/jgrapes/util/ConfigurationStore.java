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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.util.events.InitialConfiguration;

/**
 * A base class for configuration stores. Implementing classes must
 * override one of the methods {@link #structured(String)} or
 * {@link #values(String)} as the default implementations of either
 * calls the other. 
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
     * Configuration information should be kept simple. Sometimes, 
     * however, it is unavoidable to structure the information 
     * associated with a (logical) key. This can be done by 
     * reflecting the structure in the names of actual keys, derived
     * from the logical key. Names such as "key.0", "key.1", "key.2" 
     * can be used to express that the value associated with "key" 
     * is a list of values. "key.a", "key.b", "key.c" can be used 
     * to associate "key" with a map from "a", "b", "c" to some values.
     * 
     * This methods looks at all values in the mapped passed as
     * argument. If the value is a collection or map, the entry is
     * converted to several entries following the pattern outlined
     * above.
     *
     * @param structured the map with possibly structured properties
     * @return the map with flattened properties
     */
    public static Map<String, Object> flatten(Map<String, ?> structured) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> result = new HashMap<>();
        flattenObject(result, null, structured);
        return result;
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    private static void flattenObject(Map<String, Object> result,
            String prefix, Object value) {
        if (value instanceof Map) {
            for (var entry : ((Map<Object, Object>) value).entrySet()) {
                if (entry.getKey().toString().startsWith("/")) {
                    continue;
                }
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
        result.put(prefix, value);
    }

    /**
     * Same as {@link #structure(Map, boolean)} with `false` as
     * second argument.
     *
     * @param flatProperties the flat properties
     * @return a map with structured values
     */
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.ReturnEmptyCollectionRatherThanNull" })
    public static Map<String, Object> structure(Map<String, ?> flatProperties) {
        return structure(flatProperties, false);
    }

    /**
     * The reverse operation to {@link #flatten(Map)}. Entries with
     * key names matching the pattern outlined in {@link #flatten(Map)}
     * are combined to a single entry with a structured value (list or
     * map).
     * 
     * If `convertSparse` is `true` incomplete index sets such as
     * `key.2`, `key.3`, `key.5` are converted to lists with the
     * available number of elements. Else, keys with consecutive
     * indices starting with 0 must be provided. Otherwise the
     * entries are converted to a {@link Map} with keys of type
     * {@link Integer}.
     *
     * @param flatProperties the flat properties
     * @param convertSparse controls conversion to lists
     * @return a map with structured values
     */
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.ReturnEmptyCollectionRatherThanNull" })
    public static Map<String, Object>
            structure(Map<String, ?> flatProperties, boolean convertSparse) {
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
            entry.setValue(maybeConvert(entry.getValue(), convertSparse));
        }

        // Return result
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, Object> res = (Map<String, Object>) (Map) result;
        return res;
    }

    @SuppressWarnings({ "unchecked", "PMD.ConfusingTernary" })
    private static Object maybeConvert(Object value, boolean convertSparse) {
        if (!(value instanceof TreeMap)) {
            return value;
        }
        List<Object> converted = new ArrayList<>();
        for (var entry : ((Map<Object, Object>) value).entrySet()) {
            entry.setValue(maybeConvert(entry.getValue(), convertSparse));
            if (converted == null) {
                continue;
            }
            if (!(entry.getKey() instanceof Integer)
                || !convertSparse
                    && ((Integer) entry.getKey()) != converted.size()) {
                // Don't convert, leave as Map.
                converted = null;
                continue;
            }
            converted.add(entry.getValue());
        }
        return converted != null ? converted : value;
    }

    /**
     * Return the values for a given path if they exists. This
     * method should only be used in cases where configuration values
     * are needed before the {@link InitialConfiguration} event is
     * fired, e.g. while creating the component tree. 
     * 
     * @param path the path
     * @return the values, if defined for the given path
     */
    public Optional<Map<String, String>> values(String path) {
        return structured(path).map(ConfigurationStore::flatten)
            .map(o -> o.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                    e -> e.getValue().toString())));
    }

    /**
     * Return the properties for a given path if they exists
     * as structured data. 
     * 
     * @param path the path
     * @return the values, if defined for the given path
     */
    public Optional<Map<String, Object>> structured(String path) {
        return values(path).map(ConfigurationStore::structure);
    }

    /**
     * If the value is not `null`, return it as the requested type.
     * The method is successful if the value already is of the
     * requested type (or a subtype) or if the value is of type
     * {@link String} and can be converted to the requested type. 
     * 
     * Supported types are:
     * * {@link String}
     * * {@link Number}
     * 
     * @return the value
     */
    @SuppressWarnings({ "unchecked", "PMD.ShortMethodName" })
    public static <T> Optional<T> as(Object value, Class<T> requested) {
        // Handle null.
        if (value == null) {
            return Optional.empty();
        }
        // Is of requested type?
        if (requested.isAssignableFrom(value.getClass())) {
            return Optional.of((T) value);
        }
        // Convert to String, if requested.
        if (requested.equals(String.class)) {
            return Optional.of((T) value.toString());
        }
        // Remaining conversions require a string representation.
        if (!(value instanceof String)) {
            return Optional.empty();
        }
        if (requested.equals(Number.class)) {
            try {
                return Optional.of((T) new BigDecimal((String) value));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        if (requested.equals(Instant.class)) {
            try {
                return Optional.of((T) Instant.parse((String) value));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        if (requested.equals(Boolean.class)) {
            return Optional.of((T) Boolean.valueOf((String) value));
        }
        return Optional.empty();
    }

}
