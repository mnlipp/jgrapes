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

package org.jgrapes.portlets.sysinfo;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.freemarker.FreeMarkerPortlet;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

import static org.jgrapes.portal.Portlet.*;
import static org.jgrapes.portal.Portlet.RenderMode.*;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 
 */
public class SysInfoPortlet extends FreeMarkerPortlet {

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
	public SysInfoPortlet(Channel componentChannel) {
		super(componentChannel);
	}

	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceBundle(locale(channel));
		// Add SysInfoPortlet resources to page
		channel.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(PortalView.uriFromPath("SysInfo-functions.js"))
				.addCss(PortalView.uriFromPath("SysInfo-style.css"))
				.setInstantiable());
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
	 */
	@Override
	protected String generatePortletId() {
		return type() + "-" + super.generatePortletId();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#modelFromSession(org.jgrapes.io.IOSubchannel, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected <T extends Serializable> Optional<T> stateFromSession(
			Session session, String portletId, Class<T> type) {
		if (portletId.startsWith(type() + "-")) {
			return Optional.of((T)new SysInfoModel(portletId));
		}
		return Optional.empty();
	}

	@Override
	public SysInfoModel doAddPortlet(AddPortletRequest event,
			IOSubchannel channel, Session session) throws Exception {
		String portletId = generatePortletId();
		SysInfoModel portletModel = putInSession(
				session, new SysInfoModel(portletId));
		Template tpl = freemarkerConfig().getTemplate("SysInfo-preview.ftlh");
		channel.respond(new RenderPortlet(
				SysInfoPortlet.class, portletModel.getPortletId(),
				DeleteablePreview, MODES, true, templateProcessor(tpl, 
						fmModel(event, channel, portletModel))));
		return portletModel;
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet(org.jgrapes.portal.events.RenderPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected SysInfoModel doRenderPortlet(RenderPortletRequest event,
	        IOSubchannel channel, Session session, 
	        String portletId, Serializable retrievedState) throws Exception {
		SysInfoModel portletModel = (SysInfoModel)retrievedState;
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("SysInfo-preview.ftlh");
			channel.respond(new RenderPortlet(
					SysInfoPortlet.class, portletModel.getPortletId(), 
					DeleteablePreview, MODES,	event.isForeground(),
					templateProcessor(
							tpl, fmModel(event, channel, portletModel))));
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("SysInfo-view.ftlh");
			channel.respond(new RenderPortlet(
					SysInfoPortlet.class, portletModel.getPortletId(), 
					View, MODES, event.isForeground(), templateProcessor(
							tpl, fmModel(event, channel, portletModel))));
			break;
		}
		default:
			break;
		}	
		return portletModel;
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet(org.jgrapes.portal.events.DeletePortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        IOSubchannel channel, Session session, 
	        String portletId, Serializable retrievedState) throws Exception {
		channel.respond(new DeletePortlet(portletId));
	}
	
	@SuppressWarnings("serial")
	public static class SysInfoModel extends PortletBaseModel {

		/**
		 * Creates a new model with the given type and id.
		 * 
		 * @param portletId the portlet id
		 */
		@ConstructorProperties({"portletId"})
		public SysInfoModel(String portletId) {
			super(portletId);
		}

		public Properties systemProperties() {
			return System.getProperties();
		}
		
		public Runtime runtime() {
			return Runtime.getRuntime();
		}
	}
	
}
