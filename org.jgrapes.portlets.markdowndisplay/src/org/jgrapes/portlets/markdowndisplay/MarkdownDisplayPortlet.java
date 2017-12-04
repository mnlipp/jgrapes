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

package org.jgrapes.portlets.markdowndisplay;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.PortalSession;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.NotifyPortletView;
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
import java.security.Principal;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 
 */
public class MarkdownDisplayPortlet extends FreeMarkerPortlet {

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
	public MarkdownDisplayPortlet(Channel componentChannel) {
		super(componentChannel, false);
	}

	private String storagePath(Session session) {
		return "/" + session.getOrDefault(Principal.class, "").toString()
			+ "/portlets/" + MarkdownDisplayPortlet.class.getName()	+ "/";
	}
	
	@Handler
	public void onPortalReady(PortalReady event, PortalSession portalSession) 
			throws TemplateNotFoundException, MalformedTemplateNameException, 
			ParseException, IOException {
		ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
		// Add MarkdownDisplayPortlet resources to page
		portalSession.respond(new AddPortletType(type())
				.setDisplayName(resourceBundle.getString("portletName"))
				.addScript(new ScriptResource().setScriptUri(
						event.renderSupport().portletResource(type(),
								"MarkdownDisplay-functions.ftl.js")))
				.addCss(PortalView.uriFromPath("MarkdownDisplay-style.css"))
				.setInstantiable());
	}

	@Override
	public String doAddPortlet(AddPortletRequest event,
			PortalSession portalSession) throws Exception {
		String portletId = generatePortletId();
		MarkdownDisplayModel portletModel = putInSession(
				portalSession.browserSession(), 
				new MarkdownDisplayModel(portletId));
		portletModel.setPreviewContent("Preview");
		portletModel.setViewContent("View");
		Template tpl = freemarkerConfig().getTemplate(
				"MarkdownDisplay-preview.ftl.html");
		portalSession.respond(new RenderPortlet(
				MarkdownDisplayPortlet.class, portletModel.getPortletId(),
				templateProcessor(tpl, fmModel(event, portalSession, portletModel)))
				.setRenderMode(DeleteablePreview).setSupportedModes(MODES)
				.setForeground(true));
		updateView(portalSession, portletModel, portalSession.locale());
		return portletId;
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet(org.jgrapes.portal.events.RenderPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        PortalSession portalSession, String portletId, 
	        Serializable retrievedState) throws Exception {
		MarkdownDisplayModel portletModel = (MarkdownDisplayModel)retrievedState;
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("MarkdownDisplay-preview.ftl.html");
			portalSession.respond(new RenderPortlet(
					MarkdownDisplayPortlet.class, portletModel.getPortletId(), 
					templateProcessor(tpl, fmModel(event, portalSession, portletModel)))
					.setRenderMode(DeleteablePreview).setSupportedModes(MODES)
					.setForeground(event.isForeground()));
			updateView(portalSession, portletModel, portalSession.locale());
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("MarkdownDisplay-view.ftl.html");
			portalSession.respond(new RenderPortlet(
					MarkdownDisplayPortlet.class, portletModel.getPortletId(), 
					templateProcessor(tpl, fmModel(event, portalSession, portletModel)))
					.setRenderMode(View).setSupportedModes(MODES)
					.setForeground(event.isForeground()));
			updateView(portalSession, portletModel, portalSession.locale());
			break;
		}
		default:
			break;
		}
	}

	private void updateView(IOSubchannel channel, MarkdownDisplayModel model,
	        Locale locale) {
		channel.respond(new NotifyPortletView(type(),
				model.getPortletId(), "updateAll", model.getTitle(), 
				model.getPreviewContent(), model.getViewContent()));
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        PortalSession channel, String portletId, 
	        Serializable retrievedState) throws Exception {
		channel.respond(new DeletePortlet(portletId));
	}

	@SuppressWarnings("serial")
	public static class MarkdownDisplayModel extends PortletBaseModel {

		private String title;
		private String previewContent;
		private String viewContent;
		
		/**
		 * Creates a new model with the given type and id.
		 * 
		 * @param portletId the portlet id
		 */
		@ConstructorProperties({"portletId"})
		public MarkdownDisplayModel(String portletId) {
			super(portletId);
		}

		/**
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * @param title the title to set
		 */
		public void setTitle(String title) {
			this.title = title;
		}

		/**
		 * @return the previewContent
		 */
		public String getPreviewContent() {
			return previewContent;
		}

		/**
		 * @param previewContent the previewContent to set
		 */
		public void setPreviewContent(String previewContent) {
			this.previewContent = previewContent;
		}

		/**
		 * @return the viewContent
		 */
		public String getViewContent() {
			return viewContent;
		}

		/**
		 * @param viewContent the viewContent to set
		 */
		public void setViewContent(String viewContent) {
			this.viewContent = viewContent;
		}
		
		
	}
	
}
