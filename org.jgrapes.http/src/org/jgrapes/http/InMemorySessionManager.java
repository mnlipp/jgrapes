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

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.http.events.Request;

/**
 * A in memory session manager. 
 */
public class InMemorySessionManager extends SessionManager {

    @SuppressWarnings("serial")
    private final LinkedHashMap<String, Session> sessionsById
        = new LinkedHashMap<String, Session>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Entry<String, Session> eldest) {
                return maxSessions() > 0 && size() > maxSessions();
            }
        };

    /**
     * Creates a new session manager with its channel set to
     * itself and the path set to "/". The manager handles
     * all {@link Request} events.
     */
    public InMemorySessionManager() {
        this("/");
    }

    /**
     * Creates a new session manager with its channel set to
     * itself and the path set to the given path. The manager
     * handles all requests that match the given path, using the
     * same rules as browsers do for selecting the cookies that
     * are to be sent.
     * 
     * @param path the path
     */
    public InMemorySessionManager(String path) {
        this(Channel.SELF, path);
    }

    /**
     * Creates a new session manager with its channel set to
     * the given channel and the path to "/". The manager handles
     * all {@link Request} events.
     * 
     * @param componentChannel the component channel
     */
    public InMemorySessionManager(Channel componentChannel) {
        this(componentChannel, "/");
    }

    /**
     * Creates a new session manager with the given channel and path.
     * The manager handles all requests that match the given path, using
     * the same rules as browsers do for selecting the cookies that
     * are to be sent.
     *  
     * @param componentChannel the component channel
     * @param path the path
     */
    public InMemorySessionManager(Channel componentChannel, String path) {
        this(componentChannel, derivePattern(path), 1000, path);
    }

    /**
     * Creates a new session manager using the given channel and path.
     * The manager handles only requests that match the given pattern.
     * The {@link Request} handler is registered with the given priority.
     * 
     * This constructor can be used if special handling of top level
     * requests is needed.
     *
     * @param componentChannel the component channel
     * @param pattern the path part of a {@link ResourcePattern}
     * @param priority the priority
     * @param path the path
     */
    public InMemorySessionManager(Channel componentChannel, String pattern,
            int priority, String path) {
        super(componentChannel, pattern, priority, path);
    }

    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected Session createSession(String sessionId) {
        Session session = new InMemorySession(sessionId);
        Instant now = Instant.now();
        synchronized (this) {
            if (absoluteTimeout() > 0) {
                // Quick check for sessions that are too old
                for (Iterator<Entry<String, Session>> iter = sessionsById
                    .entrySet().iterator(); iter.hasNext();) {
                    Entry<String, Session> entry = iter.next();
                    if (Duration.between(entry.getValue().createdAt(), now)
                        .getSeconds() > absoluteTimeout()) {
                        iter.remove();
                    } else {
                        break;
                    }
                }
            }
            if (sessionsById.size() >= maxSessions() && absoluteTimeout() > 0) {
                // Thorough search for sessions that are too old
                sessionsById.entrySet().removeIf(entry -> {
                    return Duration.between(entry.getValue().createdAt(),
                        now).getSeconds() > absoluteTimeout()
                        || Duration.between(entry.getValue().lastUsedAt(),
                            now).getSeconds() > idleTimeout();
                });
            }
            sessionsById.put(sessionId, session);
        }
        return session;
    }

    @Override
    protected Optional<Session> lookupSession(String sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    @Override
    protected void removeSession(String sessionId) {
        synchronized (this) {
            sessionsById.remove(sessionId);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.http.SessionManager#sessionCount()
     */
    @Override
    protected int sessionCount() {
        return sessionsById.size();
    }

}
