/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.http;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;

import org.jgrapes.core.Channel;

/**
 * A in memory session manager. 
 */
public class InMemorySessionManager extends SessionManager {

	@SuppressWarnings("serial")
	private LinkedHashMap<String,Session> sessionsById 
		= new LinkedHashMap<String,Session>(16, 0.75f, true) {

			@Override
			protected boolean removeEldestEntry(Entry<String, Session> eldest) {
				return maxSessions() > 0 && size() > maxSessions();
			}
	};
	
	/**
	 * Creates a new session manager with its channel set to
	 * itself and the scope set to "/".
	 */
	public InMemorySessionManager() {
		this("/");
	}

	/**
	 * Creates a new session manager with its channel set to
	 * itself and the scope to scope.
	 * 
	 * @param scope the scope
	 */
	public InMemorySessionManager(String scope) {
		super(scope);
	}

	/**
	 * Creates a new session manager with its channel set to
	 * the given channel and the scope to "/".
	 * 
	 * @param componentChannel the component channel
	 */
	public InMemorySessionManager(Channel componentChannel) {
		super(componentChannel);
	}

	protected Session createSession(String sessionId) {
		Session session = new Session(sessionId);
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
	
	protected Optional<Session> lookupSession(String sessionId) {
		return Optional.ofNullable(sessionsById.get(sessionId));
	}
	
	protected void removeSession(String sessionId) {
		synchronized (this) {
			sessionsById.remove(sessionId);
		}
	}
}
