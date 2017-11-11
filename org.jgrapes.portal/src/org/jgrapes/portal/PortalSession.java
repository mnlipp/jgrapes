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

package org.jgrapes.portal;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;

/**
 * The server side representation of a window in the browser 
 * that displays a portal page (a portal session). An instance 
 * is created when a new portal window opens the websocket 
 * connection to the server for the first time. If the 
 * connection between the browser and the server is lost, 
 * the portal code in the browser tries to establish a 
 * new websocket connection to the same, already
 * existing {@link PortalSession}. The {@link PortalSession} 
 * object is thus independent of the websocket connection 
 * that handles the actual transfer of notifications.
 * 
 * ![Portal Session](PortalSession.svg)
 * 
 * Because there is no reliable way to be notified when a
 * window in a browser closes, {@link PortalSession}s are
 * discarded automatically unless {@link #refresh()} is called
 * before the timeout occurs.
 * 
 * {@link PortalSession} implements the {@link IOSubchannel}
 * interface. This allows the instances to be used as channels
 * for exchanging portal session scoped events with the 
 * {@link Portal} component. The upstream channel
 * (see {@link #upstreamChannel()}) is the channel of the
 * WebSocket. It may be unavailable if the connection has
 * been interrupted and not (yet) re-established.
 * 
 * As a convenience, the {@link PortalSession} provides
 * direct access to the browser session, which can 
 * usually only be obtained from the HTTP event or WebSocket
 * channel by looking for an association of type {@link Session}.
 * It also provides access to the {@link Locale} as maintained
 * by the upstream web socket.
 * 
 * @startuml PortalSession.svg
 * class PortalSession {
 *	-{static}Map<String,PortalSession> portalSessions
 *	+{static}findOrCreate(String portalSessionId, Manager component): PortalSession
 *  +setTimeout(timeout: long): PortalSession
 *  +refresh(): void
 *	+setUpstreamChannel(IOSubchannel upstreamChannel): PortalSession
 *	+setSession(Session browserSession): PortalSession
 *	+upstreamChannel(): Optional<IOSubchannel>
 *	+portalSessionId(): String
 *	+browserSession(): Session
 *	+locale(): Locale
 *	+setLocale(Locale locale): void
 * }
 * Interface IOSubchannel {
 * }
 * IOSubchannel <|.. PortalSession
 *
 * PortalSession "1" *-- "*" PortalSession : maintains
 * 
 * package org.jgrapes.http {
 *     class Session
 * }
 * 
 * PortalSession "*" -up-> "1" Session: browser session
 * @enduml
 */
public class PortalSession extends DefaultSubchannel {

	private static Map<String,PortalSession> portalSessions
		= new ConcurrentHashMap<>();
	
	private String portalSessionId;
	private boolean closed = false;
	private long timeout;
	private Timer timeoutTimer;
	private Session browserSession = null;
	private Locale locale = Locale.getDefault();
	private EventPipeline eventPipeline = null;
	// Must be weak, else there will always be a reference to the 
	// upstream channel and, through the reverseMap, to this object.
	private WeakReference<IOSubchannel> upstreamChannel = null;
	
	/**
	 * Lookup (and create if not found) the portal browserSession channel
	 * for the given portal browserSession id.
	 * 
	 * @param component the component to pass to the super 
	 * class' constructor if a new channel is created, usually 
	 * the portal
	 * @param portalSessionId the browserSession id
	 * @param timeout the portal session timeout in milli seconds
	 * @return the channel
	 */
	public static PortalSession findOrCreate(
			String portalSessionId, Manager component, long timeout) {
		return portalSessions.computeIfAbsent(portalSessionId, 
				psi -> new PortalSession(component, portalSessionId, timeout));
	}
	
	private PortalSession(Manager component, String portalSessionId,
			long timeout) {
		super(component);
		this.portalSessionId = portalSessionId;
		this.timeout = timeout;
		timeoutTimer = Components.schedule(
				t -> close(), Duration.ofMillis(timeout));
	}

	/**
	 * Changes the timeout for this {@link PortalSession} to the
	 * given value.
	 * 
	 * @param timeout the timeout in milli seconds
	 * @return the portal session for easy chaining
	 */
	public PortalSession setTimeout(long timeout) {
		this.timeout = timeout;
		timeoutTimer.reschedule(Duration.ofMillis(timeout));
		return this;
	}
	
	/**
	 * Resets the {@link PortalSession}'s timeout.
	 */
	public void refresh() {
		timeoutTimer.reschedule(Duration.ofMillis(timeout));
	}

	/**
	 * Close the portal session.
	 */
	public void close() {
		if (!closed) {
			closed = true;
			upstreamChannel().ifPresent(up -> up.respond(new Close()));
			if (eventPipeline != null) {
				eventPipeline.fire(new Closed(), PortalSession.this);
			}
			portalSessions.remove(portalSessionId);
		}
	}
	
	/**
	 * Sets the event pipeline used by upstream to send events 
	 * over this channel. This event pipeline is used to send
	 * the {@link Closed} event when the portal session times out.
	 * 
	 * @param eventPipeline the event pipeline
	 * @return the portal session for easy chaining
	 */
	public PortalSession setEventPipeline(EventPipeline eventPipeline) {
		this.eventPipeline = eventPipeline;
		return this;
	}
	
	/**
	 * Sets or updates the upstream channel. This method should only
	 * be invoked by the creator of the {@link PortalSession}, by default
	 * the {@link PortalView}.
	 * 
	 * @param upstreamChannel the upstream channel (WebSocket connection)
	 * @return the portal session for easy chaining
	 */
	public PortalSession setUpstreamChannel(IOSubchannel upstreamChannel) {
		if (upstreamChannel == null) {
			this.upstreamChannel = null;
		} else {
			this.upstreamChannel = new WeakReference<IOSubchannel>(upstreamChannel);
			upstreamChannel.associated(Selection.class).ifPresent(
					s -> locale = s.get()[0]);
		}
		return this;
	}

	/**
	 * Sets or updates associated browser session. This method should only
	 * be invoked by the creator of the {@link PortalSession}, by default
	 * the {@link PortalView}.
	 * 
	 * @param browserSession the browser session
	 * @return the portal session for easy chaining
	 */
	public PortalSession setSession(Session browserSession) {
		this.browserSession = browserSession;
		return this;
	}

	/**
	 * @return the upstream channel
	 */
	public Optional<IOSubchannel> upstreamChannel() {
		return Optional.ofNullable(upstreamChannel)
				.flatMap(weakRef -> Optional.ofNullable(weakRef.get()));
	}
	
	/**
	 * @return the portalSessionId
	 */
	public String portalSessionId() {
		return portalSessionId;
	}

	/**
	 * @return the browserSession
	 */
	public Session browserSession() {
		return browserSession;
	}

	/**
	 * Return the portal session's locale. The locale is obtained
	 * from the upstream channel set with 
	 * {@link #setUpstreamChannel(IOSubchannel)}.
	 * 
	 * @return the locale
	 */
	public Locale locale() {
		return locale;
	}

	/**
	 * Set the locale on the upstream channel.
	 * 
	 * @param locale the locale to set
	 */
	public void setLocale(Locale locale) {
		upstreamChannel().ifPresent(u -> 
			u.associated(Selection.class).ifPresent(s -> s.prefer(locale)));
	}
}
