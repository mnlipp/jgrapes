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

package org.jgrapes.http;

import java.io.Serializable;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jgrapes.core.Associator;
import org.jgrapes.http.LanguageSelector.Selection;

/**
 * Represents a browser session. Session objects are used to store
 * data related to the browser session. Servers with a small number
 * of clients usually keep session objects and the associated data
 * in memory. Servers with a large number of clients may choose to
 * only keep a LRU cache of session objects in memory and swap
 * out (persist) other session objects. Therefore the keys and
 * values stored using a session object must be serializable.
 * 
 * Occasionally, data associated with a session object is already
 * persisted anyway, because its lifetime is beyond that of a session.
 * To avoid persisting such data twice, the session provides a special
 * area for "transient data". If components choose to store data
 * in this area, they must always check before use if the data is
 * available and recover it if necessary. Using this approach, the
 * components automatically profit from the LRU caching mechanism
 * provided by the session manager. As an alternative, components
 * can use the session {@link #id()} as key to manage the data
 * on their own.
 * 
 * Before removal, the {@link SessionManager} calls the {@link #close}
 * method. The default implementation iterates through all values
 * in the transient data map and calls {@link AutoCloseable#close}
 * for each one that implements the {@link AutoCloseable} interface.
 * 
 * Implementations should override {@link Object#hashCode()}
 * and {@link Object#equals(Object)} in such a way
 * that the session id is the only relevant attribute (cannot be
 * done by default methods of the interface).
 * 
 * Note that a browser can issue several requests related to the same
 * session in parallel. Implementations of this interface must 
 * therefore ensure the thread safety of all methods provided by {@link Map}.
 * Operations that require consistency across several operations should
 * synchronize on the session object.
 */
public interface Session extends Map<Serializable, Serializable> {

    /**
     * Obtains a {@link Session} from an {@link Associator}. The
     * associator must be a {@link Request} (see 
     * {@link SessionManager#onRequest}) or a web socket channel (see 
     * {@link SessionManager#onProtocolSwitchAccepted}). As the existence of
     * an association can safely be assumed in handlers handling
     * {@link Request}s or handlers handling events on web socket channels,
     * the method returns {@link Session} and not `Optional<Session>`.
     * 
     * The method should
     * only be used in contexts where an existing association can be assumed.
     *
     * @param associator the associator
     * @return the session
     */
    @SuppressWarnings("unchecked")
    static Session from(Associator associator) {
        return ((Supplier<Session>) associator
            .associated(Session.class, Supplier.class).get()).get();
    }

    /**
     * Returns the session id.
     * 
     * @return the id
     */
    @SuppressWarnings("PMD.ShortMethodName")
    String id();

    /**
     * Returns the creation time stamp.
     * 
     * @return the creation time stamp
     */
    Instant createdAt();

    /**
     * Returns the last used (referenced in request) time stamp. 
     * 
     * @return the last used timestamp
     */
    Instant lastUsedAt();

    /**
     * Updates the last used time stamp.
     */
    void updateLastUsedAt();

    /**
     * Return the storage area for transient data. Usually implemented
     * by a {@link ConcurrentHashMap}. Other implementations must
     * at least provide the same support for concurrency as 
     * {@link ConcurrentHashMap}.
     * 
     * @return the storage area
     */
    Map<Object, Object> transientData();

    /** 
     * Iterates through all values in the transient data map and 
     * calls {@link AutoCloseable#close} for each that implements 
     * the {@link AutoCloseable} interface. Any exceptions are ignored.
     */
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.EmptyCatchBlock" })
    default void close() {
        for (Object item : transientData().values()) {
            if (item instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) item).close();
                } catch (Exception e) {
                    // Ignored
                }
            }
        }
    }

    /**
     * Convenience method for retrieving the locale 
     * set by {@link LanguageSelector} from the session.
     * 
     * @return the locale
     */
    default Locale locale() {
        return Optional.ofNullable((Selection) get(Selection.class))
            .map(selection -> selection.get()[0]).orElse(Locale.getDefault());
    }

}
