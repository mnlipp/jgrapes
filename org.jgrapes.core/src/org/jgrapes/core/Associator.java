/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implemented by classes that allow arbitrary objects to be associated
 * with instances.
 */
public interface Associator {

    /**
     * Establishes a "named" association to an associated object. Note that 
     * anything that represents an id can be used as value for 
     * parameter `name`, it does not necessarily have to be a string.
     * 
     * Passing `null` as parameter `with` clears the association.
     * 
     * @param by the "name"
     * @param with the object to be associated
     * @return the sub channel for easy chaining
     */
    @SuppressWarnings({ "PMD.ShortVariable", "PMD.AvoidDuplicateLiterals" })
    Associator setAssociated(Object by, Object with);

    /**
     * Retrieves the associated object following the association 
     * with the given "name". This general version of the method
     * supports the retrieval of values of arbitrary types
     * associated by any "name" types. 
     * 
     * @param by the "name"
     * @param type the type of the value to be retrieved
     * @param <V> the type of the value to be retrieved
     * @return the associate with the given type, if any
     */
    @SuppressWarnings("PMD.ShortVariable")
    <V> Optional<V> associated(Object by, Class<V> type);

    /**
     * Retrieves the associated object following the association 
     * with the given "name". If no association exists, the
     * object is created and the association is established.  
     * 
     * @param by the "name"
     * @param supplier the supplier
     * @param <V> the type of the value to be retrieved
     * @return the associate, if any
     */
    @SuppressWarnings({ "unchecked", "PMD.ShortVariable" })
    default <V> V associated(Object by, Supplier<V> supplier) {
        return (V) associated(by, Object.class).orElseGet(() -> {
            V associated = supplier.get();
            setAssociated(by, associated);
            return associated;
        });
    }

    /**
     * Retrieves the associated object following the association 
     * with the given name. This convenience methods simplifies the
     * retrieval of String values associated by a (real) name.
     * 
     * @param by the name
     * @return the associate, if any
     */
    @SuppressWarnings("PMD.ShortVariable")
    default Optional<String> associated(String by) {
        return associated(by, String.class);
    }

    /**
     * Retrieves the associated object following the association 
     * with the given class. The associated object must be an instance
     * of the given class.
     * 
     * @param <V> the type of the value
     * @param by the name
     * @return the associate, if any
     */
    @SuppressWarnings("PMD.ShortVariable")
    default <V> Optional<V> associated(Class<V> by) {
        return associated(by, by);
    }

}
