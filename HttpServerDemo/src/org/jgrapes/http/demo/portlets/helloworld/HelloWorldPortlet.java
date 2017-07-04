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
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
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
import static org.jgrapes.portal.Portlet.RenderMode.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 
 */
public class HelloWorldPortlet extends FreeMarkerPortlet {

	private final static Set<RenderMode> MODES = RenderMode.asSet(
			DeleteablePreview, View);
	
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
		ResourceBundle resourceBundle = resourceSupplier().apply(
				LanguageSelector.associatedLocale(channel));
		// Add HelloWorldPortlet resources to page
		channel.respond(new AddPortletType(getClass().getName())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(PortalView.uriFromPath("HelloWorld-functions.js"))
				.addCss(PortalView.uriFromPath("HelloWorld-style.css"))
				.setInstantiable());
		Collection<PortletModelBean> portletModels
			= new ArrayList<>(modelsFromSession(channel));
		if (portletModels.size() == 0) {
			portletModels.add(addToSession(channel, new HelloWorldModel()));
		}
		Map<String, Object> baseModel 
			= freemarkerBaseModel(event.renderSupport());
		for (PortletModelBean portletModel: portletModels) {
			Template tpl = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
			channel.respond(new RenderPortletFromProvider(
					portletModel.getPortletId(), DeleteablePreview, 
					MODES, newContentProvider(tpl, 
							freemarkerModel(baseModel, portletModel, channel))));
		}
	}
	
	@Handler
	public void onAddPortletRequest(AddPortletRequest event,
			IOSubchannel channel) throws TemplateNotFoundException, 
				MalformedTemplateNameException, ParseException, IOException {
		if (!event.portletType().equals(getClass().getName())) {
			return;
		}
		
		event.stop();
		HelloWorldModel portletModel 
			= addToSession(channel, new HelloWorldModel());
		Template tpl = freemarkerConfig().getTemplate("HelloWorld-preview.ftlh");
		Map<String, Object> baseModel 
			= freemarkerBaseModel(event.renderSupport());
		channel.respond(new RenderPortletFromProvider(
				portletModel.getPortletId(), DeleteablePreview, 
				MODES, newContentProvider(tpl, 
						freemarkerModel(baseModel, portletModel, channel))));
	}

	@Handler
	public void onDeletePortletRequest(DeletePortletRequest event, 
			IOSubchannel channel) {
		Optional<PortletModelBean> optPortletModel 
			= modelFromSession(channel, event.portletId());
		if (!optPortletModel.isPresent()) {
			return;
		}
	
		event.stop();
		removeFromSession(channel, optPortletModel.get());
		channel.respond(new DeletePortlet(
				optPortletModel.get().getPortletId()));
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
				portletModel.getPortletId(), View, MODES,
				newContentProvider(tpl, 
						freemarkerModel(freemarkerBaseModel(
								event.renderSupport()), portletModel, channel))));
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
