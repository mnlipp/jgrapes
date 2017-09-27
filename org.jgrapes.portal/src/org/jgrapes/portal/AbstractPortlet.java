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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Function;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletRequest;

/**
 * Provides a base class for implementing portlet components that
 * maintain the portlet's state in the session.
 * 
 * Method {@link #putInSession(Session, String, Serializable)} puts
 * arbitrary state information for the portlet type (as derived from 
 * {@link #type()}) in the session associated with the channel.
 * Method {@link #putInSession(Session, PortletBaseModel)} handles
 * state information that is derived from {@link PortletBaseModel}.
 * Deriving the portlet's state representation from this class
 * saves passing the portlet id as parameter for some invocatiosn. 
 * 
 * Method {@link #stateFromSession(Session, String, Class)} retrieves 
 * the portelt's state from the session, 
 * {@link #stateFromSession(Session, String, Function)} may also create
 * the state information if none exists.
 * 
 * {@link #removeFromSession(Session, String)} removes
 * the state information.
 * 
 * Using these methods, this class also provides basic event handlers that
 * implement e.g. the necessary lookup of a session required by every portlet
 * that uses portlet state information.
 */
public abstract class AbstractPortlet extends Component {	
	
	/**
	 * Creates a new component that listens for new events
	 * on the given channel.
	 * 
	 * @param channel the channel to listen on
	 */
	public AbstractPortlet(Channel channel) {
		super(channel);
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
	 * Generates a new unique portlet id.
	 * 
	 * @return the portlet id
	 */
	protected String generatePortletId() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Returns the locale associated with the channel or the default
	 * locale if none is associated.
	 * 
	 * @param channel the channel
	 * @return the locale
	 */
	protected Locale locale(IOSubchannel channel) {
		return channel.associated(Selection.class)
				.map(s -> s.get()[0]).orElse(Locale.getDefault());
	}
	
	/**
	 * A default handler for resource requests. Checks that the request
	 * is directed at this portlet, and calls {@link #doGetResource}.
	 * 
	 * @param event the resource request event
	 * @param channel the channel that the request was recived on
	 */
	@Handler
	public void onResourceRequest(
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
	 */
	protected void doGetResource(PortletResourceRequest event,
			IOSubchannel channel) {
		InputStream stream = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (stream == null) {
			return;
		}
		
		// Found resource, send.
		channel.respond(new PortletResourceResponse(event));
		new InputStreamPipeline(stream, channel).suppressClose().run();
		event.setResult(true);
	}
	
	/**
	 * Provides resource bundle for localization.
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
	 * Puts the given portlet model in the session using the 
	 * {@link #type()} and the model's id as keys.
	 * 
	 * @param session the session to use
	 * @param portletModel the portlet model
	 * @return the portlet model
	 */
	protected <T extends PortletBaseModel> T putInSession(
			Session session, T portletModel) {
			return putInSession(
					session, portletModel.getPortletId(), portletModel);
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
	 * Returns the portlet state of this portlet's type with the given id
	 * from the session, creating it if it doesn't exist. The function
	 * creating the state is invoked with the portlet id.
	 * 
	 * This method uses {@link #stateFromSession(Session, String, Class)}
	 * to find out if the state information exists already.
	 * 
	 * @param session the session to use
	 * @param portletId the portlet id
	 * @param supplier the function that creates a new state
	 * @return the portlet state
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> T stateFromSession(
			Session session, String portletId, Function<String,T> supplier) {
		return ((Optional<T>)stateFromSession(
				session, portletId, Serializable.class))
				.orElse(putInSession(
						session, portletId, supplier.apply(portletId)));
	}

	/**
	 * Removes the portlet state of the portlet with the given id
	 * from the session.
	 * 
	 * @param session the session to use
	 * @param portletId the portlet id
	 */
	@SuppressWarnings("unchecked")
	protected void removeFromSession(
			Session session, String portletId) {
		((Map<Serializable,Map<Serializable,Map<String,Serializable>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>())
			.remove(portletId);
	}

	/**
	 * Removes the portlet with the given model
	 * from the session.
	 * 
	 * @param session the session to use
	 * @param portletModel the portlet model
	 * @return the portlet model
	 */
	@SuppressWarnings("unchecked")
	protected <T extends PortletBaseModel> T removeFromSession(
			Session session, T portletModel) {
		((Map<Serializable,Map<Serializable,Map<String,Serializable>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>())
			.remove(portletModel.getPortletId());
		return portletModel;
	}

	/**
	 * Remove all models belonging to this type of portlet
	 * from the session.
	 * 
	 * @param session the session
	 */
	@SuppressWarnings("unchecked")
	protected void removeAllModelsFromSession(Session session) {
		((Map<Serializable,Map<Serializable,Map<String,PortletBaseModel>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>()).clear();
	}
	
	/**
	 * Checks if the request applies to this component. If so, stops the event,
	 * and calls {@link #doAddPortlet}. 
	 * 
	 * @param event the event
	 * @param channel the channel
	 */
	@Handler
	public void onAddPortletRequest(AddPortletRequest event,
			IOSubchannel channel) throws Exception {
		if (!event.portletType().equals(type())) {
			return;
		}
		event.stop();
		Session session = session(channel);
		doAddPortlet(event, channel, session);
	}

	/**
	 * Called by {@link #onAddPortletRequest} to complete adding the portlet.
	 * If the portlet has associated state, the implementation should
	 * call {@link #stateFromSession(Session, String, Function)} to create
	 * the state and put it in the session.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param session the session
	 */
	protected abstract void doAddPortlet(AddPortletRequest event, 
			IOSubchannel channel, Session session) throws Exception;
	
	/**
	 * Checks if the request applies to this component. If so, stops the event,
	 * removes the portlet model bean from the session
	 * and calls {@link #doDeletePortlet}
	 * with the model. 
	 * 
	 * @param event the event
	 * @param channel the channel
	 */
	@Handler
	public void onDeletePortletRequest(DeletePortletRequest event, 
			IOSubchannel channel) throws Exception {
		Session session = session(channel);
		Optional<? extends Serializable> optPortletState 
			= stateFromSession(session, event.portletId(), Serializable.class);
		if (!optPortletState.isPresent()) {
			return;
		}
		event.stop();
		Serializable portletState = optPortletState.get();
		removeFromSession(session, event.portletId());
		doDeletePortlet(event, channel, session, event.portletId(), portletState);
	}
	
	/**
	 * Called by {@link #onDeletePortletRequest} to complete deleting
	 * the portlet.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param session the session
	 * @param portletId the portlet id
	 * @param portletState the portlet state
	 */
	protected abstract void doDeletePortlet(DeletePortletRequest event, 
			IOSubchannel channel, Session session, String portletId, 
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
	 * @param channel the channel
	 */
	@Handler
	public void onRenderPortlet(RenderPortletRequest event,
			IOSubchannel channel) throws Exception {
		Session session = session(channel);
		Optional<? extends Serializable> optPortletState 
			= stateFromSession(session, event.portletId(), Serializable.class);
		if (!optPortletState.isPresent()) {
			return;
		}
		event.stop();
		doRenderPortlet(event, channel, session, 
				event.portletId(), optPortletState.get());
	}

	/**
	 * Called by {@link #onRenderPortlet} to complete rendering
	 * the portlet.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param portletId the portlet id
	 * @param portletState the portletState
	 */
	protected abstract void doRenderPortlet(RenderPortletRequest event, 
			IOSubchannel channel, Session session, String portletId, 
			Serializable portletState) throws Exception;
	
	/**
	 * Convenience method that returns the session associated with
	 * the channel.
	 * 
	 * @return the session
	 * @throws IllegalStateException if no session is found
	 */
	public Session session(IOSubchannel channel) {
		return channel.associated(Session.class).orElseThrow(
				() -> new IllegalStateException("Session is missing."));
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
