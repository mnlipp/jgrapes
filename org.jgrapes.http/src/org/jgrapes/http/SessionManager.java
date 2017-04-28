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

import java.net.HttpCookie;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;

import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.CacheControlDirectives;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.CookieList;
import org.jdrupes.httpcodec.types.Directive;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.http.events.Request;

/**
 * A in memory session manager. The event is associated with a 
 * {@link Session} object using `Session.class` as association identifier.
 * 
 * @see EventBase#setAssociated(Object, Object)
 * @see "[OWASP Session Management Cheat Sheet](https://www.owasp.org/index.php/Session_Management_Cheat_Sheet)"
 */
public class SessionManager extends Component {

	private static SecureRandom secureRandom = new SecureRandom();
	
	private String idName = "id";
	@SuppressWarnings("serial")
	private LinkedHashMap<String,Session> sessionsById 
		= new LinkedHashMap<String,Session>(16, 0.75f, true) {

			@Override
			protected boolean removeEldestEntry(Entry<String, Session> eldest) {
				return SessionManager.this.maxSessions > 0
						&& size() > SessionManager.this.maxSessions;
			}
	};
	private int maxSessionAge = -1;
	private int maxSessions = 1000;
	
	/**
	 * Creates a new session manager with its channel set to
	 * itself.
	 */
	public SessionManager() {
	}

	/**
	 * Creates a new component base with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel
	 */
	public SessionManager(Channel componentChannel) {
		super(componentChannel);
	}

	/**
	 * The name used for the session id cookie. Defaults to "`id`".
	 * 
	 * @return the id name
	 */
	public String idName() {
		return idName;
	}

	/**
	 * @param idName the id name to set
	 * 
	 * @return the session manager for easy chaining
	 */
	public SessionManager setIdName(String idName) {
		this.idName = idName;
		return this;
	}

	/**
	 * Set the maximum number of sessions. If the value is zero or less,
	 * an unlimited number of sessions is supported. The default value
	 * is 1000.
	 * 
	 * If adding a new session would exceed the limit, first all
	 * sessions older than {@link #maxSessionAge()} are removed.
	 * If this doesn't free a slot, the least recently used session
	 * is removed.
	 * 
	 * @param maxSessions the maxSessions to set
	 * @return the session manager for easy chaining
	 */
	public SessionManager setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
		return this;
	}

	/**
	 * @return the maxSessions
	 */
	public int maxSessions() {
		return maxSessions;
	}

	/**
	 * Sets the maximum age for a session in seconds. 
	 * 
	 * @param maxSessionAge the sessionMaxAge to set
	 * @return the session manager for easy chaining
	 */
	public SessionManager setMaxSessionAge(int maxSessionAge) {
		this.maxSessionAge = maxSessionAge;
		return this;
	}

	/**
	 * @return the maximum session age
	 */
	public int maxSessionAge() {
		return maxSessionAge;
	}

	/**
	 * Associates the event with a {@link Session} object
	 * using `Session.class` as association identifier.
	 * 
	 * @param event the event
	 */
	@Handler(priority=1000)
	public void onRequest(Request event) {
		final HttpRequest request = event.request();
		Optional<String> requestedSessionId = request.findValue(
		        HttpField.COOKIE, Converters.COOKIE_LIST)
		        .flatMap(cookies -> cookies.stream().filter(
		                cookie -> cookie.getName().equals(idName))
		                .findFirst().map(HttpCookie::getValue));
		if (requestedSessionId.isPresent()) {
			String sessionId = requestedSessionId.get();
			synchronized(SessionManager.this) {
				Session session = sessionsById.get(sessionId);
				if (session != null) {
					if (maxSessionAge <= 0
						|| Duration.between(session.createdAt(), Instant.now())
							.getSeconds() < maxSessionAge) {
						event.setAssociated(Session.class, session);
						return;
					}
					// Invalidate, too old 
					sessionsById.remove(sessionId);
				}
			}
		}
		String sessionId = createSessionId(request.response().get());
		Session session = createSession(sessionId);
		event.setAssociated(Session.class, session);
	}

	private Session createSession(String sessionId) {
		Session session = new Session(this, sessionId);
		Instant now = Instant.now();
		synchronized (this) {
			if (maxSessionAge > 0) {
				// Quick check for sessions that are too old
				for (Iterator<Entry<String, Session>> iter = sessionsById
				        .entrySet().iterator(); iter.hasNext();) {
					Entry<String, Session> entry = iter.next();
					if (Duration.between(entry.getValue().createdAt(), now)
					        .getSeconds() > maxSessionAge) {
						iter.remove();
					} else {
						break;
					}
				}
			}
			if (sessionsById.size() >= maxSessions && maxSessionAge > 0) {
				// Thorough search for sessions that are too old
				sessionsById.entrySet().removeIf(entry -> {
					return Duration.between(entry.getValue().createdAt(),
					        now).getSeconds() > maxSessionAge;
				});
			}
			sessionsById.put(sessionId, session);
		}
		return session;
	}
	
	private String createSessionId(HttpResponse response) {
		StringBuilder sessionIdBuilder = new StringBuilder();
		byte[] bytes = new byte[16];
		secureRandom.nextBytes(bytes);
		for (byte b: bytes) {
			sessionIdBuilder.append(Integer.toHexString(b & 0xff));
		}
		String sessionId = sessionIdBuilder.toString();
		HttpCookie sessionCookie = new HttpCookie(idName, sessionId);
		sessionCookie.setHttpOnly(true);
		response.computeIfAbsent(HttpField.SET_COOKIE, CookieList::new)
			.value().add(sessionCookie);
		response.computeIfAbsent(
				HttpField.CACHE_CONTROL, CacheControlDirectives::new)
			.value().add(new Directive("no-cache", "SetCookie, Set-Cookie2"));
		return sessionId;
	}
	
	/**
	 * Discards the given session.
	 * 
	 * @param session the session
	 */
	void discard(Session session) {
		synchronized (this) {
			sessionsById.remove(session).id();
		}
	}
}
