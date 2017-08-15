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

import java.io.InputStream;
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
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.PortletResourceResponse;

/**
 * Provides a base class for implementing portlet components. 
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
	 * A default handler for resource requests. Searches for
	 * a file with the requested URI in the portlets class path. 
	 * 
	 * @param event the resource request event
	 * @param channel the channel that the request was recived on
	 */
	@Handler
	public void onResourceRequest(
			PortletResourceRequest event, IOSubchannel channel) {
		// For me?
		if (!event.portletClass().equals(getClass().getName())) {
			return;
		}
		
		// Look for content
		InputStream in = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (in == null) {
			return;
		}

		// Respond
		channel.respond(new PortletResourceResponse(event, in));
		
		// Done
		event.setResult(true);
	}

	/**
	 * Returns all portlet models of this portlet's class from the
	 * session.
	 * 
	 * @param channel the channel, used to access the session
	 * @return the models
	 */
	@SuppressWarnings("unchecked")
	protected Collection<PortletModelBean> modelsFromSession(
			IOSubchannel channel) {
		return channel.associated(Session.class).map(session ->
			((Map<String,PortletModelBean>)session.computeIfAbsent(getClass(),
					k -> new HashMap<>())).values())
			.orElseThrow(() -> new IllegalStateException("Session is missing."));
	}
	
	/**
	 * Returns the portlet model of this portlet's class with the given id
	 * from the session.
	 * 
	 * @param channel the channel, used to access the session
	 * @param portletId the portel id
	 * @return the models
	 */
	@SuppressWarnings("unchecked")
	protected Optional<PortletModelBean> modelFromSession(
			IOSubchannel channel, String portletId) {
		return channel.associated(Session.class).map(session ->
			Optional.ofNullable(
					((Map<String,PortletModelBean>)session.computeIfAbsent(
							getClass(), k -> new HashMap<>()))
					.get(portletId)))
			.orElseThrow(() -> new IllegalStateException("Session is missing."));
	}

	/**
	 * Adds the given portlet model to the session.
	 * 
	 * @param channel the channel, used to access the session
	 * @param model the model
	 * @return the model
	 */
	@SuppressWarnings("unchecked")
	protected <T extends PortletModelBean> T addToSession(
			IOSubchannel channel, T model) {
		Optional<Session> optSession = channel.associated(Session.class);
		if (!optSession.isPresent()) {
			throw new IllegalStateException("Session is missing.");
		}
		((Map<String,PortletModelBean>)optSession.get()
				.computeIfAbsent(getClass(), k -> new HashMap<>()))
			.put(model.getPortletId(), model);
		return model;
	}
	
	/**
	 * Removes the given portlet model from the session.
	 * 
	 * @param channel the channel, used to access the session
	 * @param model the model
	 * @return the model
	 */
	@SuppressWarnings("unchecked")
	protected <T extends PortletModelBean> T removeFromSession(
			IOSubchannel channel, T model) {
		Optional<Session> optSession = channel.associated(Session.class);
		if (!optSession.isPresent()) {
			throw new IllegalStateException("Session is missing.");
		}
		((Map<String,PortletModelBean>)optSession.get()
				.computeIfAbsent(getClass(), k -> new HashMap<>()))
			.remove(model.getPortletId());
		return model;
	}
	
	/**
	 * Provides a supplier for a resource bundle for localization.
	 * The default implementation looks up a bundle using the
	 * package name plus "l10n" as base name.
	 * 
	 * @return the supplier
	 */
	protected Function<Locale,ResourceBundle> resourceSupplier() {
		return locale -> ResourceBundle.getBundle(
			getClass().getPackage().getName() + ".l10n", locale, 
			getClass().getClassLoader(),
				ResourceBundle.Control.getNoFallbackControl(
						ResourceBundle.Control.FORMAT_DEFAULT));
		
	}
	
	/**
	 * Defines the portlet model following the JavaBean conventions.
	 * Portlet models should follow these cpnventions because
	 * many template engines rely on this. 
	 */
	public abstract static class PortletModelBean {

		private String portletId;

		/**
		 * Creates a new model with a new unique portlet id.
		 */
		public PortletModelBean() {
			portletId = UUID.randomUUID().toString();
		}

		/**
		 * Creates a new model with the given portlet id.
		 * 
		 * @param portletId the portlet id
		 */
		public PortletModelBean(String portletId) {
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
