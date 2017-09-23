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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.LanguageSelector.Selection;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletRequest;

/**
 * Provides a base class for implementing portlet components that
 * maintain a model for each portlet as a JavaBean in the session.
 * 
 * Method {@link #addToSession} adds
 * a model for the portlet type (as derived from {@link #type()}) 
 * to the session associated with the channel. Method
 * {@link #modelFromSession} retrieves a model,
 * {@link AbstractPortlet#removeFromSession}
 * removes it.
 * 
 * Using these methods, this class also provides basic event handlers that
 * implement e.g. the necessary lookup of a session required by every portlet
 * that uses a portlet model.
 * 
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
	 * Creates new portlet model bean. Should be 
	 * overridden by sub classes that need a derived model with more 
	 * information than the base model.
	 * 
	 * The default implementation creates a model with
	 * `new PortletBaseModel(generatePortletId())`.
	 * 
	 * @return the model bean
	 */
	protected PortletBaseModel createPortletModel() {
		PortletBaseModel portletModel = new PortletBaseModel(
				generatePortletId());
		return portletModel;
	}
	
	/**
	 * A default handler for resource requests. Checks that the request
	 * is directed at this portlet, calls {@link #doGetResource}
	 * and sends the response event. 
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
		
		InputStream in = doGetResource(event);
		if (in == null) {
			return;
		}

		// Respond
		channel.respond(new PortletResourceResponse(event, in));
		
		// Done
		event.setResult(true);
	}

	/**
	 * Tries to find an input stream that delivers the requested
	 * resource. The default implementation searches for
	 * a file with the requested URI in the portlets class path.
	 * 
	 * @param event the event
	 * @return the input stream or `null`
	 */
	protected InputStream doGetResource(PortletResourceRequest event) {
		return this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
	}
	
	/**
	 * Returns all portlet models of this portlet's type from the
	 * session.
	 * 
	 * @param channel the channel, used to access the session
	 * @return the models
	 */
	@SuppressWarnings("unchecked")
	protected Collection<PortletBaseModel> modelsFromSession(
			IOSubchannel channel) {
		return channel.associated(Session.class).map(session ->
				((Map<Serializable,Map<Serializable,Map<String,PortletBaseModel>>>)
						(Map<Serializable,?>)session)
				.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
				.computeIfAbsent(type(), t -> new HashMap<>()).values())
			.orElseThrow(() -> new IllegalStateException("Session is missing."));
	}
	
	/**
	 * Returns the portlet model of this portlet's type with the given id
	 * from the session.
	 * 
	 * @param session the session to use
	 * @param portletId the portel id
	 * @return the models
	 */
	@SuppressWarnings("unchecked")
	protected Optional<? extends PortletBaseModel> modelFromSession(
			Session session, String portletId) {
		return Optional.ofNullable(
				((Map<Serializable,Map<Serializable,Map<String,PortletBaseModel>>>)
						(Map<Serializable,?>)session)
				.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
				.computeIfAbsent(type(), t -> new HashMap<>())
				.get(portletId));
	}

	/**
	 * Adds the given portlet model to the session using the {@link #type()}
	 * and the model's {@link PortletBaseModel#getPortletId()} as keys.
	 * 
	 * @param session the session to use
	 * @param model the model
	 * @return the model
	 */
	@SuppressWarnings("unchecked")
	protected <T extends PortletBaseModel> T addToSession(
			Session session, T model) {
		((Map<Serializable,Map<Serializable,Map<String,PortletBaseModel>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>())
			.put(model.getPortletId(), model);
		return model;
	}
	
	/**
	 * Removes the given portlet model from the session.
	 * 
	 * @param session the session to use
	 * @param model the model
	 * @return the model
	 */
	@SuppressWarnings("unchecked")
	protected <T extends PortletBaseModel> T removeFromSession(
			Session session, T model) {
		((Map<Serializable,Map<Serializable,Map<String,PortletBaseModel>>>)
				(Map<Serializable,?>)session)
			.computeIfAbsent(AbstractPortlet.class, ac -> new HashMap<>())
			.computeIfAbsent(type(), t -> new HashMap<>())
			.remove(model.getPortletId());
		return model;
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
	 * Checks if the request applies to this component. If so, stops the event,
	 * calls {@link #createPortletModel()} to create a new model bean, adds it
	 * to the session and call {@link #doAddPortlet}
	 * with the newly created model bean. 
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
		PortletBaseModel portletModel= addToSession(
				session, createPortletModel());
		doAddPortlet(event, channel, session, portletModel);
	}

	/**
	 * Called by {@link #onAddPortletRequest} to complete adding the portlet.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param session the session
	 * @param portletModel the model bean
	 */
	protected abstract void doAddPortlet(AddPortletRequest event, 
			IOSubchannel channel, Session session, 
			PortletBaseModel portletModel) throws Exception;
	
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
		Optional<? extends PortletBaseModel> optPortletModel 
			= modelFromSession(session, event.portletId());
		if (!optPortletModel.isPresent()) {
			return;
		}
		event.stop();
		PortletBaseModel portletModel = optPortletModel.get();
		removeFromSession(session, portletModel);
		doDeletePortlet(event, channel, session, portletModel);
	}
	
	/**
	 * Called by {@link #onDeletePortletRequest} to complete deleting
	 * the portlet.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param session the session
	 * @param portletModel the model bean
	 */
	protected abstract void doDeletePortlet(DeletePortletRequest event, 
			IOSubchannel channel, Session session, 
			PortletBaseModel portletModel) throws Exception;
	
	/**
	 * Checks if the request applies to this component by calling
	 * {@link #modelFromSession}. If a model
	 * is found, stops the event, and calls 
	 * {@link #doRenderPortlet} with the model. 
	 * 
	 * Some portlets that do not persist their models between sessions
	 * (e.g. because the model only references data maintained elsewhere)
	 * should override {@link #modelFromSession}
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
		Optional<? extends PortletBaseModel> optPortletModel 
			= modelFromSession(session, event.portletId());
		if (!optPortletModel.isPresent()) {
			return;
		}
		event.stop();
		doRenderPortlet(event, channel, session, optPortletModel.get());
	}

	/**
	 * Called by {@link #onRenderPortlet} to complete rendering
	 * the portlet.
	 * 
	 * @param event the event
	 * @param channel the channel
	 * @param portletModel the model bean
	 */
	protected abstract void doRenderPortlet(RenderPortletRequest event, 
			IOSubchannel channel, Session session, 
			PortletBaseModel portletModel) throws Exception;
	
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
	}
}
