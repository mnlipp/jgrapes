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

package org.jgrapes.http.demo.portlets.helloworld;

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionLock;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.UserPrincipal;
import org.jgrapes.portal.Utils;
import org.jgrapes.portal.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.NotifyPortletModel;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.freemarker.FreeMarkerPortlet;
import org.jgrapes.util.events.KeyValueStoreData;
import org.jgrapes.util.events.KeyValueStoreQuery;
import org.jgrapes.util.events.KeyValueStoreUpdate;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import static org.jgrapes.portal.Portlet.*;
import static org.jgrapes.portal.Portlet.RenderMode.*;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 
 */
public class HelloWorldPortlet extends FreeMarkerPortlet {

	private final static Set<RenderMode> MODES = RenderMode.asSet(
			DeleteablePreview, View);
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	public HelloWorldPortlet(Channel componentChannel) {
		super(componentChannel, false);
	}

	private String storagePath(Session session) {
		return "/" + Utils.userFromSession(session)
			.map(UserPrincipal::toString).orElse("")
			+ "/portlets/" + HelloWorldPortlet.class.getName()	+ "/";
	}
	
	@Handler
	public void onPortalReady(PortalReady event, PortalSession portalSession) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
		// Add HelloWorldPortlet resources to page
		portalSession.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(new ScriptResource().setScriptUri(
						event.renderSupport().portletResource(type(),
								"HelloWorld-functions.js")))
				.addCss(PortalView.uriFromPath("HelloWorld-style.css"))
				.setInstantiable());
		KeyValueStoreQuery query = new KeyValueStoreQuery(
				storagePath(portalSession.browserSession()), true);
		portalSession.setAssociated(
				HelloWorldPortlet.class, new CompletionLock(event, 3000));
		fire(query, portalSession);
	}

	@Handler
	public void onKeyValueStoreData(
			KeyValueStoreData event, PortalSession channel) 
					throws JsonDecodeException {
		if (!event.event().query().equals(storagePath(channel.browserSession()))) {
			return;
		}
		channel.associated(HelloWorldPortlet.class, CompletionLock.class)
			.ifPresent(lock -> lock.remove());
		for (String json: event.data().values()) {
			HelloWorldModel model = JsonBeanDecoder.create(json)
					.readObject(HelloWorldModel.class);
			putInSession(channel.browserSession(), model);
		}
	}
	
	@Override
	public String doAddPortlet(AddPortletRequest event,
			PortalSession channel) throws Exception {
		String portletId = generatePortletId();
		HelloWorldModel portletModel = putInSession
				(channel.browserSession(), new HelloWorldModel(portletId));
		String jsonState = JsonBeanEncoder.create()
				.writeObject(portletModel).toJson();
		channel.respond(new KeyValueStoreUpdate().update(
				storagePath(channel.browserSession()) + portletModel.getPortletId(),
				jsonState));
		Template tpl = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
		channel.respond(new RenderPortlet(
				HelloWorldPortlet.class, portletModel.getPortletId(),
				templateProcessor(tpl, fmModel(event, channel, portletModel)))
				.setRenderMode(DeleteablePreview).setSupportedModes(MODES)
				.setForeground(true));
		return portletId;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet(org.jgrapes.portal.events.DeletePortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        PortalSession channel, String portletId, 
	        Serializable portletState) throws Exception {
		channel.respond(new KeyValueStoreUpdate().delete(
				storagePath(channel.browserSession()) + portletId));
		channel.respond(new DeletePortlet(portletId));
	}
	
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet(org.jgrapes.portal.events.RenderPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        PortalSession channel, String portletId, 
	        Serializable retrievedState) throws Exception {
		HelloWorldModel portletModel = (HelloWorldModel)retrievedState;
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
			channel.respond(new RenderPortlet(
					HelloWorldPortlet.class, portletModel.getPortletId(), 
					templateProcessor(tpl, fmModel(event, channel, portletModel)))
					.setRenderMode(DeleteablePreview).setSupportedModes(MODES)
					.setForeground(event.isForeground()));
;
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("HelloWorld-view.ftlh");
			channel.respond(new RenderPortlet(
					HelloWorldPortlet.class, portletModel.getPortletId(), 
					templateProcessor(tpl, fmModel(event, channel, portletModel)))
					.setRenderMode(View).setSupportedModes(MODES)
					.setForeground(event.isForeground()));
			channel.respond(new NotifyPortletView(type(),
					portletModel.getPortletId(), "setWorldVisible",
					portletModel.isWorldVisible()));
			break;
		}
		default:
			break;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doNotifyPortletModel(org.jgrapes.portal.events.NotifyPortletModel, org.jgrapes.io.IOSubchannel, org.jgrapes.http.Session, java.io.Serializable)
	 */
	@Override
	protected void doNotifyPortletModel(NotifyPortletModel event,
	        PortalSession channel, Serializable portletState)
	        throws Exception {
		event.stop();
		HelloWorldModel portletModel = (HelloWorldModel)portletState;
		portletModel.setWorldVisible(!portletModel.isWorldVisible());
		
		String jsonState = JsonBeanEncoder.create()
				.writeObject(portletModel).toJson();
		channel.respond(new KeyValueStoreUpdate().update(
				storagePath(channel.browserSession()) + portletModel.getPortletId(),
				jsonState));
		channel.respond(new NotifyPortletView(type(),
				portletModel.getPortletId(), "setWorldVisible", 
				portletModel.isWorldVisible()));
	}

	@SuppressWarnings("serial")
	public static class HelloWorldModel extends PortletBaseModel {

		private boolean worldVisible = true;
		
		/**
		 * Creates a new model with the given type and id.
		 * 
		 * @param portletId the portlet id
		 */
		@ConstructorProperties({"portletId"})
		public HelloWorldModel(String portletId) {
			super(portletId);
		}

		/**
		 * @param visible the visible to set
		 */
		public void setWorldVisible(boolean visible) {
			this.worldVisible = visible;
		}

		public boolean isWorldVisible() {
			return worldVisible;
		}
	}
	
}
