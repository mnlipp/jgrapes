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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapes.core.Manager;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultSubchannel;

/**
 * The server side representation of a window in the browser 
 * that displays a portal view (a portal session). An instance 
 * is created when a new portal window opens the websocket 
 * connection to the server for the first time. If the 
 * connection between the browser and the server is lost, 
 * the portal code in the browser tries to establish a 
 * new websocket connection to the same, already
 * existing {@link PortalSession}. The {@link PortalSession} 
 * object is thus independent of the websocket connection 
 * that handles the actual transfer of notifications.
 * 
 * {@link PortalSession} implements the {@link IOSubchannel}
 * interface. This allows the instances to be used as channels
 * for exchanging portal browserSession scoped events with the 
 * {@link Portal} component. The upstream channel
 * (see {@link #upstreamChannel()}) is the channel of the
 * WebSocket. It may be unavailable if the connection has
 * been interrupted and not re-established.
 * 
 * As a convenience, the {@link PortalSession} provides
 * direct access to the browser session, which can 
 * usually only be obtained from the HTTP event or WebSocket
 * channel by looking for an association of type {@link Session}.
 */
public class PortalSession extends DefaultSubchannel {

	private static Map<String,PortalSession> portalSessions
		= new ConcurrentHashMap<>();
	
	private String portalSessionId;
	private Session browserSession = null;
	private Locale locale = Locale.getDefault();
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
	 * @return the channel
	 */
	public static PortalSession findOrCreate(
			String portalSessionId, Manager component) {
		return portalSessions.computeIfAbsent(portalSessionId, 
				psi -> new PortalSession(component, portalSessionId));
	}
	
	private PortalSession(Manager component, String portalSessionId) {
		super(component);
		this.portalSessionId = portalSessionId;
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
			locale = upstreamChannel.associated(Selection.class)
					.map(s -> s.get()[0]).orElse(Locale.getDefault());
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
}
