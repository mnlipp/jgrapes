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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.RenderSupport;
import org.jgrapes.portal.events.AddPortletResources;
import org.jgrapes.portal.events.NotifyPortletModel;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortletFromProvider;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.freemarker.FreeMarkerPortlet;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import static org.jgrapes.portal.Portlet.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class HelloWorldPortlet extends FreeMarkerPortlet {

	private String portletId;
	
	/**
	 * Creates a new component with its channel set to
	 * itself.
	 */
	public HelloWorldPortlet() {
		this(Channel.SELF);
	}

	protected String portletId() {
		return portletId;
	}
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	public HelloWorldPortlet(Channel componentChannel) {
		super(componentChannel);
		portletId = Components.objectFullName(this);
	}

	private boolean isWorldVisible(Map<Object,Object> portletSession) {
		return (boolean)portletSession
				.computeIfAbsent("WorldVisible", k -> true);
	}
	
	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		channel.respond(new AddPortletResources(getClass().getName())
				.addScript(PortalView.uriFromPath("HelloWorld-functions.js"))
				.addCss(PortalView.uriFromPath("HelloWorld-style.css")));
		Template tpl = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
		channel.respond(new RenderPortletFromProvider(
				portletId, "Hello World", RenderMode.Preview, 
				VIEWABLE_PORTLET_MODES, newContentProvider(
						tpl, fmModel(channel, event.renderSupport()))));
	}
	
	@Handler
	public void onRenderPortlet(RenderPortletRequest event,
			IOSubchannel channel) 
					throws TemplateNotFoundException, 
					MalformedTemplateNameException, ParseException, 
					IOException {
		if (!event.portletId().equals(portletId)) {
			return;
		}
		
		event.stop();
		Template tpl = freemarkerConfig().getTemplate("HelloWorld-view.ftlh");
		channel.respond(new RenderPortletFromProvider(
				portletId, "Hello World", RenderMode.View, 
				VIEWABLE_PORTLET_MODES, newContentProvider(
						tpl, fmModel(channel, event.renderSupport()))));
		Map<Object,Object> session = portletSession(channel);
		channel.respond(new NotifyPortletView(getClass().getName(),
				portletId, "setWorldVisible", isWorldVisible(session)));
	}
	
	@Handler
	public void onChangePortletModel(NotifyPortletModel event,
			IOSubchannel channel) throws TemplateNotFoundException, 
			MalformedTemplateNameException, ParseException, IOException {
		if (!event.portletId().equals(portletId)) {
			return;
		}
		event.stop();
		
		Map<Object,Object> session = portletSession(channel);
		boolean visible = !isWorldVisible(session);
		session.put("WorldVisible", visible);
		
		channel.respond(new NotifyPortletView(getClass().getName(),
				portletId, "setWorldVisible", visible));
	}
	
	private Map<Object,Object> fmModel(
			IOSubchannel channel, RenderSupport renderSupport) {
		return channel.associated(getClass().getName() + "#fmModel", () -> {
			Map<Object,Object> model = new HashMap<>(
					freemarkerBaseModel(renderSupport));
			model.put("portlet", new PortletBeanView(channel));
			return model;
		});
		
	}

	public class PortletBeanView {
		
		private Map<Object,Object> portletSession;
		
		public PortletBeanView(IOSubchannel channel) {
			portletSession = portletSession(channel);
		}
		
		public boolean isWorldVisible() {
			return HelloWorldPortlet.this.isWorldVisible(portletSession);
		}
	}
	
}
