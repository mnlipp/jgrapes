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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.http.events.DiscardSession;
import org.jgrapes.http.events.Request;

/**
 * A in memory session manager. 
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class InMemorySessionManager extends SessionManager {

    @SuppressWarnings({ "serial", "PMD.UseConcurrentHashMap" })
    private final Map<String, InMemorySession> sessionsById
        = new LinkedHashMap<>(16, 0.75f, true) {

            @Override
            protected boolean
                    removeEldestEntry(Entry<String, InMemorySession> eldest) {
                if (maxSessions() > 0 && size() > maxSessions()
                    && eldest.getValue().setBeingDiscarded()) {
                    fire(new DiscardSession(eldest.getValue()));
                }
                return false;
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
     * the given channel, the path to "/" and the handler's priority 
     * to 1000. The manager handles all {@link Request} events.
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
     * are to be sent. The request handler's priority is set to 1000.
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
    @SuppressWarnings({ "PMD.CognitiveComplexity",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    protected Optional<Instant> startDiscarding(long absoluteTimeout,
            long idleTimeout) {
        synchronized (this) {
            Instant nextTimout = null;
            for (InMemorySession session : sessionsById.values()) {
                if (hasTimedOut(session)) {
                    if (session.setBeingDiscarded()) {
                        fire(new DiscardSession(session));
                    }
                    continue;
                }
                if (absoluteTimeout > 0) {
                    Instant timesOutAt = session.createdAt()
                        .plus(Duration.ofMillis(absoluteTimeout));
                    if (nextTimout == null
                        || timesOutAt.isBefore(nextTimout)) {
                        nextTimout = timesOutAt;
                    }
                }
                if (idleTimeout > 0) {
                    Instant timesOutAt = session.lastUsedAt()
                        .plus(Duration.ofMillis(idleTimeout));
                    if (nextTimout == null
                        || timesOutAt.isBefore(nextTimout)) {
                        nextTimout = timesOutAt;
                    }
                }
            }
            return Optional.ofNullable(nextTimout);
        }
    }

    @Override
    protected Session createSession(String sessionId) {
        InMemorySession session = new InMemorySession(sessionId);
        synchronized (this) {
            sessionsById.put(sessionId, session);
        }
        return session;
    }

    @Override
    protected Optional<Session> lookupSession(String sessionId) {
        synchronized (this) {
            return Optional.ofNullable(sessionsById.get(sessionId));
        }
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
        synchronized (this) {
            return sessionsById.size();
        }
    }

}
