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
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.PortalView;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * 
 */
public class HelloWorldPortlet extends FreeMarkerPortlet {

	/**
	 * Creates a new component with its channel set to
	 * itself.
	 */
	public HelloWorldPortlet() {
		this(Channel.SELF);
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
	}

	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		// Add HelloWorldPortlet resources to page
		channel.respond(new AddPortletResources(getClass().getName())
				.addScript(PortalView.uriFromPath("HelloWorld-functions.js"))
				.addCss(PortalView.uriFromPath("HelloWorld-style.css")));
		Collection<PortletModelBean> portletModels
			= new ArrayList<>(modelsFromSession(channel));
		if (portletModels.size() == 0) {
			portletModels.add(addToSession(channel, new HelloWorldModel()));
		}
		for (PortletModelBean portletModel: portletModels) {
			Template tpl = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
			channel.respond(new RenderPortletFromProvider(
					portletModel.getPortletId(), RenderMode.Preview, 
					VIEWABLE_PORTLET_MODES, newContentProvider(
							tpl, event.renderSupport(), portletModel)));
		}
	}
	
	@Handler
	public void onRenderPortlet(RenderPortletRequest event,
			IOSubchannel channel) 
					throws TemplateNotFoundException, 
					MalformedTemplateNameException, ParseException, 
					IOException {
		Optional<PortletModelBean> optPortletModel 
			= modelFromSession(channel, event.portletId());
		if (!optPortletModel.isPresent()) {
			return;
		}
		
		event.stop();
		HelloWorldModel portletModel = (HelloWorldModel)optPortletModel.get();
		Template tpl = freemarkerConfig().getTemplate("HelloWorld-view.ftlh");
		channel.respond(new RenderPortletFromProvider(
				portletModel.getPortletId(), RenderMode.View, 
				VIEWABLE_PORTLET_MODES, newContentProvider(tpl, 
						event.renderSupport(), portletModel)));
		channel.respond(new NotifyPortletView(getClass().getName(),
				portletModel.getPortletId(), "setWorldVisible",
				portletModel.isWorldVisible()));
	}
	
	@Handler
	public void onChangePortletModel(NotifyPortletModel event,
			IOSubchannel channel) throws TemplateNotFoundException, 
			MalformedTemplateNameException, ParseException, IOException {
		Optional<PortletModelBean> optPortletModel 
			= modelFromSession(channel, event.portletId());
		if (!optPortletModel.isPresent()) {
			return;
		}
	
		event.stop();
		HelloWorldModel portletModel = (HelloWorldModel)optPortletModel.get();
		portletModel.setVisible(!portletModel.isWorldVisible());
		
		channel.respond(new NotifyPortletView(getClass().getName(),
				portletModel.getPortletId(), "setWorldVisible", 
				portletModel.isWorldVisible()));
	}
	
	public class HelloWorldModel extends PortletModelBean {

		private boolean visible = true;
		
		public HelloWorldModel() {
		}
		
		/**
		 * @param visible the visible to set
		 */
		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		public boolean isWorldVisible() {
			return visible;
		}
	}
	
}
