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
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.DiscardSession;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.io.IOSubchannel;

/**
 * A in memory session manager. The event is associated with a 
 * {@link Session} object using `Session.class` as association identifier.
 * 
 * The manager tracks requests with the specified scope. The scope is a
 * URL prefix that has to be matched by the request, usually "/".
 * If no cookie with the id name (see {@link #idName()}) is found,
 * a new cookie with that name and a path equal to the scope is created.
 * The cookies values is the unique session id that is used to lookup
 * the session object.
 * 
 * @see EventBase#setAssociated(Object, Object)
 * @see "[OWASP Session Management Cheat Sheet](https://www.owasp.org/index.php/Session_Management_Cheat_Sheet)"
 */
public class InMemorySessionManager extends Component {

	private static SecureRandom secureRandom = new SecureRandom();

	private String path = "/";
	private String idName = "id";
	@SuppressWarnings("serial")
	private LinkedHashMap<String,Session> sessionsById 
		= new LinkedHashMap<String,Session>(16, 0.75f, true) {

			@Override
			protected boolean removeEldestEntry(Entry<String, Session> eldest) {
				return InMemorySessionManager.this.maxSessions > 0
						&& size() > InMemorySessionManager.this.maxSessions;
			}
	};
	private int absoluteTimeout = 9*60*60;
	private int idleTimeout = 30*60;
	private int maxSessions = 1000;
	
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
		this(Channel.SELF, scope);
	}

	/**
	 * Creates a new session manager with its channel set to
	 * the given channel and the scope to "/".
	 * 
	 * @param componentChannel the component channel
	 */
	public InMemorySessionManager(Channel componentChannel) {
		this(componentChannel, "/");
	}

	/**
	 * Creates a new session manager with its channel set to the given 
	 * channel and the path set to path.
	 * 
	 * @param componentChannel the component channel
	 * @param scope the scope
	 */
	public InMemorySessionManager(Channel componentChannel, String scope) {
		super(componentChannel);
		if (scope.equals("/") || !scope.endsWith("/")) {
			path = scope;
		} else {
			path = scope.substring(0, scope.length() - 1);
		}
		String pattern;
		if (path.equals("/")) {
			pattern = "/**";
		} else {
			pattern = path + "," + path + "/**";
		}
		RequestHandler.Evaluator.add(this, "onRequest", pattern);
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
	public InMemorySessionManager setIdName(String idName) {
		this.idName = idName;
		return this;
	}

	/**
	 * Set the maximum number of sessions. If the value is zero or less,
	 * an unlimited number of sessions is supported. The default value
	 * is 1000.
	 * 
	 * If adding a new session would exceed the limit, first all
	 * sessions older than {@link #absoluteTimeout()} are removed.
	 * If this doesn't free a slot, the least recently used session
	 * is removed.
	 * 
	 * @param maxSessions the maxSessions to set
	 * @return the session manager for easy chaining
	 */
	public InMemorySessionManager setMaxSessions(int maxSessions) {
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
	 * Sets the absolute timeout for a session in seconds. The absolute
	 * timeout is the time after which a session is invalidated (relative
	 * to its creation time). Defaults to 9 hours. Zero or less disables
	 * the timeout.
	 * 
	 * @param absoluteTimeout the absolute timeout
	 * @return the session manager for easy chaining
	 */
	public InMemorySessionManager setAbsoluteTimeout(int absoluteTimeout) {
		this.absoluteTimeout = absoluteTimeout;
		return this;
	}

	/**
	 * @return the absolute session timeout (in seconds)
	 */
	public int absoluteTimeout() {
		return absoluteTimeout;
	}

	/**
	 * Sets the idle timeout for a session in seconds. Defaults to 30 minutes.
	 * Zero or less disables the timeout. 
	 * 
	 * @param idleTimeout the absolute timeout
	 * @return the session manager for easy chaining
	 */
	public InMemorySessionManager setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
		return this;
	}

	/**
	 * @return the idle timeout (in seconds)
	 */
	public int idleTimeout() {
		return idleTimeout;
	}

	/**
	 * Associates the event with a {@link Session} object
	 * using `Session.class` as association identifier.
	 * 
	 * @param event the event
	 */
	@RequestHandler(priority=1000, dynamic=true)
	public void onRequest(Request event) {
		final HttpRequest request = event.httpRequest();
		Optional<String> requestedSessionId = request.findValue(
		        HttpField.COOKIE, Converters.COOKIE_LIST)
		        .flatMap(cookies -> cookies.stream().filter(
		                cookie -> cookie.getName().equals(idName))
		                .findFirst().map(HttpCookie::getValue));
		if (requestedSessionId.isPresent()) {
			String sessionId = requestedSessionId.get();
			synchronized(InMemorySessionManager.this) {
				Session session = sessionsById.get(sessionId);
				Instant now = Instant.now();
				if (session != null) {
					if ((absoluteTimeout <= 0
							|| Duration.between(session.createdAt(), 
									now).getSeconds() < absoluteTimeout)
						&& (idleTimeout <= 0
							|| Duration.between(session.lastUsedAt(),
									now).getSeconds() < idleTimeout)) {
						event.setAssociated(Session.class, session);
						session.updateLastUsedAt();
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
		Session session = new Session(sessionId);
		Instant now = Instant.now();
		synchronized (this) {
			if (absoluteTimeout > 0) {
				// Quick check for sessions that are too old
				for (Iterator<Entry<String, Session>> iter = sessionsById
				        .entrySet().iterator(); iter.hasNext();) {
					Entry<String, Session> entry = iter.next();
					if (Duration.between(entry.getValue().createdAt(), now)
					        .getSeconds() > absoluteTimeout) {
						iter.remove();
					} else {
						break;
					}
				}
			}
			if (sessionsById.size() >= maxSessions && absoluteTimeout > 0) {
				// Thorough search for sessions that are too old
				sessionsById.entrySet().removeIf(entry -> {
					return Duration.between(entry.getValue().createdAt(),
								now).getSeconds() > absoluteTimeout
							|| Duration.between(entry.getValue().lastUsedAt(),
								now).getSeconds() > idleTimeout;
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
		sessionCookie.setPath("/");
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
	 * @param event the event
	 */
	@Handler(channels=Channel.class)
	public void discard(DiscardSession event) {
		synchronized (this) {
			sessionsById.remove(event.session().id());
		}
	}
	
	@Handler(priority=1000)
	public void onWebSocketAccepted(
			WebSocketAccepted event, IOSubchannel channel) {
		event.requestEvent().associated(Session.class)
			.ifPresent(session -> 
				channel.setAssociated(Session.class, session));
	}
}
