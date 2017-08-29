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

package org.jgrapes.osgi.portlets.bundles;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
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
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 
 */
public class BundleListPortlet extends FreeMarkerPortlet {

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
	public BundleListPortlet(Channel componentChannel) {
		super(componentChannel);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#generatePortletId()
	 */
	@Override
	protected String generatePortletId() {
		return type() + "-" + super.generatePortletId();
	}

	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceSupplier().apply(
				LanguageSelector.associatedLocale(channel));
		// Add portlet resources to page
		channel.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.setInstantiable());
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#modelFromSession(org.jgrapes.io.IOSubchannel, java.lang.String)
	 */
	@Override
	protected Optional<PortletBaseModel> modelFromSession(
			Session session, String portletId) {
		Optional<PortletBaseModel> optModel 
			= super.modelFromSession(session, portletId);
		if (optModel.isPresent()) {
			return optModel;
		}
		if (portletId.startsWith(type() + "-")) {
			return Optional.of(addToSession(
					session, new PortletBaseModel(portletId)));
		}
		return Optional.empty();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doAddPortlet(org.jgrapes.portal.events.AddPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doAddPortlet(AddPortletRequest event, IOSubchannel channel,
	        Session session, PortletBaseModel portletModel) throws Exception {
		Template tpl = freemarkerConfig().getTemplate("Bundles-preview.ftlh");
		Map<String, Object> baseModel 
			= freemarkerBaseModel(event.renderSupport());
		channel.respond(new RenderPortletFromProvider(
				BundleListPortlet.class, portletModel.getPortletId(),
				DeleteablePreview, MODES, newContentProvider(tpl, 
						freemarkerModel(baseModel, portletModel, channel)),
				true));
	}

	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet(org.jgrapes.portal.events.DeletePortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        IOSubchannel channel, Session session, PortletBaseModel portletModel)
	        throws Exception {
		channel.respond(new DeletePortlet(portletModel.getPortletId()));
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet(org.jgrapes.portal.events.RenderPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        IOSubchannel channel, Session session, PortletBaseModel retrievedModel)
	        throws Exception {
		if (!event.portletId().startsWith(getClass().getName() + "-")) {
			return;
		}
		
		Map<String, Object> baseModel 
			= freemarkerBaseModel(event.renderSupport());
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("Bundles-preview.ftlh");
			channel.respond(new RenderPortletFromProvider(
					BundleListPortlet.class, retrievedModel.getPortletId(), 
					DeleteablePreview, MODES,	newContentProvider(tpl, 
							freemarkerModel(baseModel, retrievedModel, channel)),
					event.isForeground()));
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("Bundles-view.ftlh");
			channel.respond(new RenderPortletFromProvider(
					BundleListPortlet.class, retrievedModel.getPortletId(), 
					View, MODES, newContentProvider(tpl, 
							freemarkerModel(baseModel, retrievedModel, channel)),
					event.isForeground()));
			break;
		}
		default:
			break;
		}	
	}
	
}
