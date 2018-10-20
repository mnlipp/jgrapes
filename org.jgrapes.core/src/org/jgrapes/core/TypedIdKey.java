/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.core;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * Combines a type and an id value to a key for an {@link Associator}
 * or {@link Map}. This kind of key can be used if an instance of 
 * the type as key is too specific (or objects should not be kept alive
 * only because they are used as keys) and the identifier alone cannot 
 * be guaranteed to be unique.
 */
@SuppressWarnings({ "serial", "PMD.ShortVariable" })
public final class TypedIdKey<V> implements Serializable {

    private final Class<V> type;
    private final Serializable id;

    private TypedIdKey(Class<V> type, Serializable id) {
        this.type = type;
        this.id = id;
    }

    /**
     * Associates the given value's type and the id with the given value
     * using the given associator. 
     *
     * @param <V> the value type
     * @param associator the associator
     * @param id the id
     * @param value the value
     * @return the value for easy chaining
     */
    @SuppressWarnings({ "unchecked" })
    public static <V> V associate(Associator associator, Serializable id,
            V value) {
        associator.setAssociated(
            new TypedIdKey<V>((Class<V>) value.getClass(), id), value);
        return value;
    }

    /**
     * Associates the given value's type and the id with the given value
     * using the given map. 
     *
     * @param <V> the value type
     * @param map the map
     * @param id the id
     * @param value the value
     * @return the value for easy chaining
     */
    @SuppressWarnings({ "unchecked" })
    public static <V> V put(Map<? super TypedIdKey<V>, ? super V> map,
            Serializable id, V value) {
        map.put(new TypedIdKey<V>((Class<V>) value.getClass(), id), value);
        return value;
    }

    /**
     * Retrieves a value with the given type and id from the given associator.
     *
     * @param <V> the value type
     * @param associator the associator
     * @param type the type
     * @param id the id
     * @return the associated value, if any
     */
    public static <V> Optional<V> associated(Associator associator,
            Class<V> type, Serializable id) {
        return associator.associated(new TypedIdKey<>(type, id), type);
    }

    /**
     * Retrieves a value with the given type and id from the given map.
     *
     * @param <V> the value type
     * @param map the map
     * @param type the type
     * @param id the id
     * @return the associated value, if any
     */
    @SuppressWarnings({ "unchecked" })
    public static <V> Optional<V> get(Map<?, ?> map, Class<V> type,
            Serializable id) {
        return Optional
            .ofNullable((V) map.get(new TypedIdKey<>(type, id)));
    }

    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public int hashCode() {
        @SuppressWarnings("PMD.AvoidFinalLocalVariable")
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypedIdKey<?> other = (TypedIdKey<?>) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TypedIdKey [" + type + ":" + id + "]";
    }
}
