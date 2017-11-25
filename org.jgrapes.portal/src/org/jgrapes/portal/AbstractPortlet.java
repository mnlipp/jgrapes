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

import java.beans.ConstructorProperties;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.NotifyPortletModel;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletRequest;

/**
 * Provides a base class for implementing portlet components that
 * maintain the portlet's state in the browser session.
 * 
 * Method {@link #putInSession(Session, String, Serializable)} puts
 * arbitrary state information for the portlet type (obtained from 
 * {@link #type()}) in the browser session associated with the channel.
 * 
 * Method {@link #stateFromSession(Session, String, Class)}
 * retrieves the state information from the session if it exists.
 * 
 * Method {@link #removeState(Session, String)} removes
 * any state information associated with the given portlet id.
 * 
 * Using these methods, the class provides basic event handlers that
 * perform e.g. the necessary lookup of the state information and
 * pass it to the methods to be implemented by the derived
 * portlet (see {@link #doAddPortlet}, {@link #doDeletePortlet},
 * {@link #doRenderPortlet} etc.).
 * 
 * In addition, the class provides support for tracking the 
 * relationship between {@link PortalSession}s and the ids 
 * of portlets displayed in the portal session and support for
 * periodic updates.
 */
public abstract class AbstractPortlet extends Component {	

	private Map<PortalSession,Set<String>> portletIdsByPortalSession = null;
	private Duration refreshInterval = null;
	private Timer refreshTimer = null;

	/**
	 * Creates a new component that listens for new events
	 * on the given channel.
	 * 
	 * @param channel the channel to listen on
	 * @param trackPortalSessions if set, track the relationship between
	 * portal sessions and portlet ids
	 */
	public AbstractPortlet(Channel channel, boolean trackPortalSessions) {
		super(channel);
		if (trackPortalSessions) {
			portletIdsByPortalSession = Collections.synchronizedMap(
					new WeakHashMap<>());
		}
	}

	/**
	 * If set to a value different from `null` causes 
	 * {@link #doRefreshPortletViews()} to be called periodically
	 * if at least one {@link PortalSession} is being tracked.
	 * 
	 * @param interval the refresh interval
	 * @return the portlet for easy chaining
	 */
	public AbstractPortlet setPeriodicRefresh(Duration interval) {
		refreshInterval = interval;
		if (refreshTimer != null) {
			refreshTimer.cancel();
			refreshTimer = null;
		}
		updateRefresh();
		return this;
	}
	
	private void updateRefresh() {
		if (refreshInterval == null || portletIdsByPortalSession == null
				|| portletIdsByPortalSession.size() == 0) {
			// At least one of the prerequisits is missing, terminate
			if (refreshTimer != null) {
				refreshTimer.cancel();
				refreshTimer = null;
			}
			return;
		}
		if (refreshTimer != null) {
			// Already running.
			return;
		}
		refreshTimer = Components.schedule(t -> {
			t.reschedule(t.scheduledFor().plus(refreshInterval));
			doRefreshPortletViews();
		}, Instant.now().plus(refreshInterval));
	}

	/**
	 * Called periodically if periodical refreshs are enabled.
	 * See {@link #setPeriodicRefresh(Duration)}. The default
	 * implementation does nothing.
	 */
	protected void doRefreshPortletViews() {
	}
	
	/**
	 * Returns the portlet type. The default implementation
	 * returns the class' name.
	 * 
	 * @return the type
	 */
	protected String type() {
		return getClass().getName();
	}
	
	/**
	 * A default handler for resource requests. Checks that the request
	 * is directed at this portlet, and calls {@link #doGetResource}.
	 * 
	 * @param event the resource request event
	 * @param channel the channel that the request was recived on
	 */
	@Handler
	public final void onResourceRequest(
			PortletResourceRequest event, IOSubchannel channel) {
		// For me?
		if (!event.portletClass().equals(type())) {
			return;
		}
		doGetResource(event, channel);
	}

	/**
	 * Generates the reponse event for a resource request
	 * (a {@link PortletResourceResponse} and at least one
	 * {@link Output} event.
	 * 
	 * The default implementation searches for
	 * a file with the requested URI in the portlets class path
	 * and sends its content if found.
	 * 
	 * @param event the event. The result will be set to
	 * `true` on success
	 * @param channel the channel
	 */
	protected void doGetResource(PortletResourceRequest event,
			IOSubchannel channel) {
		InputStream stream = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (stream == null) {
			return;
		}
		
		// Resource found, send.
		channel.respond(new PortletResourceResponse(event, false));
		new InputStreamPipeline(stream, channel).suppressClose().run();
		event.setResult(true);
	}
	
	/**
	 * Provides a resource bundle for localization.
	 * The default implementation looks up a bundle using the
	 * package name plus "l10n" as base name.
	 * 
	 * @return the resource bundle
	 */
	protected ResourceBundle resourceBundle(Locale locale) {
		return ResourceBundle.getBundle(
			getClass().getPackage().getName() + ".l10n", locale, 
			getClass().getClassLoader(),
				ResourceBundle.Control.getNoFallbackControl(
						ResourceBundle.Control.FORMAT_DEFAULT));
	}

	/**
	 * Generates a new unique portlet id.
	 * 
	 * @return the portlet id
	 */
	protected String generatePortletId() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Returns the tracked models and channels.
	 * 
	 * @return the result
	 */
	protected Map<PortalSession,Set<String>> portletIdsByPortalSession() {
		// Create copy to get a non-weak map.
		if (portletIdsByPortalSession != null) {
			return new HashMap<>(portletIdsByPortalSession);
		}
		return Collections.emptyMap();
	}
	
	/**
	 * Puts the given portlet state in the session using the 
	 * {@link #type()} and the given portlet id as keys.
	 * 
	 * @param session the session to use
	 * @param portletId the portlet id
	 * @param portletState the portlet state
	 * @return the portlet state
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> T putInSession(
			Session session, String portletId, T portletState) {
		((Map<Serializable,Map<Serializable,Map<String,Serializable>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>())
			.put(portletId, portletState);
		return portletState;
	}

	/**
	 * Puts the given portlet state in the session using the 
	 * {@link #type()} and the portlet id from the model.
	 * 
	 * @param session the session to use
	 * @param portletModel the portlet model
	 * @return the portlet model
	 */
	protected <T extends PortletBaseModel> T putInSession(
			Session session, T portletModel) {
		return putInSession(session, portletModel.getPortletId(), portletModel);
	}

	/**
	 * Returns the portlet state of this portlet's type with the given id
	 * from the session.
	 * 
	 * @param session the session to use
	 * @param portletId the portlet id
	 * @param type the state's type
	 * @return the portlet state
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> Optional<T> stateFromSession(
			Session session, String portletId, Class<T> type) {
		return Optional.ofNullable(
				((Map<Serializable,Map<Serializable,Map<String,T>>>)
						(Map<Serializable,?>)session)
				.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
				.computeIfAbsent(type(), t -> new HashMap<>())
				.get(portletId));
	}

	/**
	 * Returns all portlet states of this portlet's type from the
	 * session.
	 * 
	 * @param channel the channel, used to access the session
	 * @param type the states' type
	 * @return the states
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> Collection<T> statesFromSession(
			IOSubchannel channel, Class<T> type) {
		return channel.associated(Session.class).map(session ->
				((Map<Serializable,Map<Serializable,Map<String,T>>>)
						(Map<Serializable,?>)session)
				.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
				.computeIfAbsent(type(), t -> new HashMap<>()).values())
			.orElseThrow(() -> new IllegalStateException("Session is missing."));
	}
	
	/**
	 * Removes the portlet state of the portlet with the given id
	 * from the session. 
	 * 
	 * @param session the session to use
	 * @param portletId the portlet id
	 * @return the removed state if state existed
	 */
	@SuppressWarnings("unchecked")
	protected Optional<? extends Serializable> removeState(
			Session session, String portletId) {
		Serializable state = ((Map<Serializable,Map<Serializable,Map<String,Serializable>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>())
			.remove(portletId);
		return Optional.ofNullable(state);
	}

	/**
	 * Returns the set of portlet ids associated with the portal session.
	 * The set is created if it doesn't exist yet.
	 * 
	 * @param portalSession the portal session
	 * @return the set
	 */
	private Set<String> portletIdsOfPortalSession(PortalSession portalSession) {
		if (portletIdsByPortalSession != null) {
			return portletIdsByPortalSession.computeIfAbsent(
					portalSession, ps -> new HashSet<>());
		}
		return Collections.emptySet();
	}
	
	/**
	 * Checks if the request applies to this component. If so, stops the event,
	 * and calls {@link #doAddPortlet}. 
	 * 
	 * @param event the event
	 * @param portalSession the channel
	 */
	@Handler
	public final void onAddPortletRequest(AddPortletRequest event,
			PortalSession portalSession) throws Exception {
		if (!event.portletType().equals(type())) {
			return;
		}
		event.stop();
		String portletId = doAddPortlet(event, portalSession);
		event.setResult(portletId);
		if (portletIdsByPortalSession != null) {
			portletIdsOfPortalSession(portalSession).add(portletId);
			updateRefresh();
		}
	}

	/**
	 * Called by {@link #onAddPortletRequest} to complete adding the portlet.
	 * If the portlet has associated state, the implementation should
	 * call {@link #putInSession(Session, String, Serializable)} to create
	 * the state and put it in the session.
	 * 
	 * @param event the event
	 * @param portalSession the channel
	 * @return the id of the created portlet
	 */
	protected abstract String doAddPortlet(AddPortletRequest event, 
			PortalSession portalSession) throws Exception;
	
	/**
	 * Checks if the request applies to this component. If so, stops 
	 * the event, removes the portlet state from the browser session
	 * and calls {@link #doDeletePortlet} with the state.
	 * 
	 * If the association of {@link PortalSession}s and portlet ids
	 * is tracked for this portlet, any existing association is
	 * also removed.
	 * 
	 * @param event the event
	 * @param portalSession the portal session
	 */
	@Handler
	public final void onDeletePortletRequest(DeletePortletRequest event, 
			PortalSession portalSession) throws Exception {
		String portletId = event.portletId();
		Optional<? extends Serializable> optPortletState 
			= stateFromSession(portalSession.browserSession(), 
					event.portletId(), Serializable.class);
		if (!optPortletState.isPresent()) {
			return;
		}
		removeState(portalSession.browserSession(), portletId);
		if (!optPortletState.isPresent()) {
			return;
		}
		if (portletIdsByPortalSession != null) {
			for (Iterator<PortalSession> psi = portletIdsByPortalSession
					.keySet().iterator(); psi.hasNext();) {
				Set<String> idSet = portletIdsByPortalSession.get(psi.next());
				idSet.remove(portletId);
				if (idSet.isEmpty()) {
					psi.remove();
				}
			}
			updateRefresh();
		}
		event.stop();
		doDeletePortlet(event, portalSession, event.portletId(), optPortletState.get());
	}
	
	/**
	 * Called by {@link #onDeletePortletRequest} to complete deleting
	 * the portlet.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param portletId the portlet id
	 * @param portletState the portlet state
	 */
	protected abstract void doDeletePortlet(DeletePortletRequest event, 
			PortalSession channel, String portletId, 
			Serializable portletState) throws Exception;
	
	/**
	 * Checks if the request applies to this component by calling
	 * {@link #stateFromSession(Session, String, Class)}. If a model
	 * is found, stops the event, and calls 
	 * {@link #doRenderPortlet} with the state information. 
	 * 
	 * Some portlets that do not persist their models between sessions
	 * (e.g. because the model only references data maintained elsewhere)
	 * should override {@link #stateFromSession(Session, String, Class)}
	 * in such a way that it creates the requested model if it doesn't 
	 * exist yet.
	 * 
	 * @param event the event
	 * @param portalSession the portal session
	 */
	@Handler
	public final void onRenderPortlet(RenderPortletRequest event,
			PortalSession portalSession) throws Exception {
		Optional<? extends Serializable> optPortletState 
			= stateFromSession(portalSession.browserSession(), 
					event.portletId(), Serializable.class);
		if (!optPortletState.isPresent()) {
			return;
		}
		event.stop();
		if (portletIdsByPortalSession != null) {
			portletIdsOfPortalSession(portalSession).add(event.portletId());
			updateRefresh();
		}
		doRenderPortlet(event, portalSession, event.portletId(), optPortletState.get());
	}

	/**
	 * Called by {@link #onRenderPortlet} to complete rendering
	 * the portlet.
	 * 
	 * If the method returns the state information, an association between
	 * the state and the channel will be created.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param portletId the portlet id
	 * @param portletState the portletState
	 */
	protected abstract void doRenderPortlet(RenderPortletRequest event, 
			PortalSession channel, String portletId, 
			Serializable portletState) throws Exception;
	
	/**
	 * Checks if the request applies to this component by calling
	 * {@link #stateFromSession(Session, String, Class)}. If a model
	 * is found, calls {@link #doNotifyPortletModel} with the state 
	 * information. 
	 * 
	 * @param event the event
	 * @param channel the channel
	 */
	@Handler
	public final void onNotifyPortletModel(NotifyPortletModel event,
			PortalSession channel) throws Exception {
		Optional<? extends Serializable> optPortletState 
			= stateFromSession(channel.browserSession(), event.portletId(), Serializable.class);
		if (!optPortletState.isPresent()) {
			return;
		}
		doNotifyPortletModel(event, channel, optPortletState.get());
	}

	/**
	 * Called by {@link #onNotifyPortletModel} to complete handling
	 * the notification. The default implementation does nothing.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param portletState the portletState
	 */
	protected void doNotifyPortletModel(NotifyPortletModel event, 
			PortalSession channel, Serializable portletState)
					throws Exception {
	}
	
	/**
	 * Removes the {@link PortalSession} from the set of tracked sessions.
	 * If derived portlets need to perform extra actions when a
	 * portalSession is closed, they have to override 
	 * {@link #afterOnClosed(Closed, PortalSession)}.
	 * 
	 * @param event the closed event
	 * @param portalSession the portal session
	 */
	@Handler
	public final void onClosed(Closed event, PortalSession portalSession) {
		if (portletIdsByPortalSession != null) {
			portletIdsByPortalSession.remove(portalSession);
			updateRefresh();
		}
		afterOnClosed(event, portalSession);
	}

	/**
	 * Invoked by {@link #onClosed(Closed, PortalSession)} after
	 * the portal session has been removed from the set of
	 * tracked sessions. The default implementation does
	 * nothing.
	 * 
	 * @param event the closed event
	 * @param portalSession the portal session
	 */
	protected void afterOnClosed(Closed event, PortalSession portalSession) {
	}
	
	/**
	 * Defines the portlet model following the JavaBean conventions.
	 * 
	 * Portlet models should follow these conventions because
	 * many template engines rely on them and to support serialization
	 * to portable formats. 
	 */
	@SuppressWarnings("serial")
	public static class PortletBaseModel implements Serializable {

		protected String portletId;

		/**
		 * Creates a new model with the given type and id.
		 * 
		 * @param portletId the portlet id
		 */
		@ConstructorProperties({"portletId"})
		public PortletBaseModel(String portletId) {
			this.portletId = portletId;
		}

		/**
		 * Returns the portlet id.
		 * 
		 * @return the portlet id
		 */
		public String getPortletId() {
			return portletId;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
			        + ((portletId == null) ? 0 : portletId.hashCode());
			return result;
		}

		/**
		 * Two objects are equal if they have equal portlet ids.
		 * 
		 * @param obj the other object
		 * @return the result
		 */
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
			PortletBaseModel other = (PortletBaseModel) obj;
			if (portletId == null) {
				if (other.portletId != null) {
					return false;
				}
			} else if (!portletId.equals(other.portletId)) {
				return false;
			}
			return true;
		}
	}
}
