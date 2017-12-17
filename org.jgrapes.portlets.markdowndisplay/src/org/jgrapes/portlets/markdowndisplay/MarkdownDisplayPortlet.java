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

import org.jdrupes.json.JsonBeanDecoder;
import org.jdrupes.json.JsonBeanEncoder;
import org.jdrupes.json.JsonDecodeException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionLock;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.io.IOSubchannel;
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
import org.jgrapes.portal.events.UpdatePortletModel;
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
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * A portlet used to display information to the user. Instances
 * may be used as a kind of note, i.e. created and configured by
 * a user himself. A typical use case, however, is to create
 * an instance during startup by a portal policy.
 */
public class MarkdownDisplayPortlet extends FreeMarkerPortlet {

	/**
	 * The supported preferences.
	 */
	public enum Preferences { PORTLET_ID, TITLE, PREVIEW_SOURCE, 
		VIEW_SOURCE, DELETABLE, EDITABLE_BY };
	
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
		return "/" + Utils.userFromSession(session)
			.map(UserPrincipal::toString).orElse("")
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
				.addScript(new ScriptResource()
						.setRequires(new String[] {"markdown-it.github.io",
								"github.com/markdown-it/markdown-it-abbr",
								"github.com/markdown-it/markdown-it-container",
								"github.com/markdown-it/markdown-it-deflist",
								"github.com/markdown-it/markdown-it-emoji",
								"github.com/markdown-it/markdown-it-footnote",
								"github.com/markdown-it/markdown-it-ins",
								"github.com/markdown-it/markdown-it-mark",
								"github.com/markdown-it/markdown-it-sub",
								"github.com/markdown-it/markdown-it-sup"})
						.setScriptUri(event.renderSupport().portletResource(
								type(), "MarkdownDisplay-functions.ftl.js")))
				.addCss(PortalView.uriFromPath("MarkdownDisplay-style.css"))
				.setInstantiable());
		KeyValueStoreQuery query = new KeyValueStoreQuery(
				storagePath(portalSession.browserSession()), true);
		portalSession.setAssociated(
				MarkdownDisplayPortlet.class, new CompletionLock(event, 3000));
		fire(query, portalSession);
	}

	@Handler
	public void onKeyValueStoreData(
			KeyValueStoreData event, PortalSession channel) 
					throws JsonDecodeException {
		if (!event.event().query().equals(storagePath(channel.browserSession()))) {
			return;
		}
		channel.associated(MarkdownDisplayPortlet.class, CompletionLock.class)
			.ifPresent(lock -> lock.remove());
		for (String json: event.data().values()) {
			MarkdownDisplayModel model = JsonBeanDecoder.create(json)
					.readObject(MarkdownDisplayModel.class);
			putInSession(channel.browserSession(), model);
		}
	}

	/**
	 * Adds the portlet to the portal. The portlet supports the 
	 * following options (see {@link AddPortletRequest#properties()}
	 * and {@link Preferences}):
	 * 
	 * * `PORTLET_ID` (String): The portlet id.
	 * 
	 * * `TITLE` (String): The portlet title.
	 * 
	 * * `PREVIEW_SOURCE` (String): The markdown source that is rendered 
	 * in the portlet preview.
	 * 
	 * * `VIEW_SOURCE` (String): The markdown source that is rendered 
	 * in the portlet view.
	 * 
	 * * `DELETABLE` (Boolean): Indicates that the portlet may be 
	 * deleted from the overview page.
	 * 
	 * * `EDITABLE_BY` (Set&lt;Principal&gt;): The principals that may edit 
	 * the portlet instance.
	 */
	@Override
	public String doAddPortlet(AddPortletRequest event,
			PortalSession portalSession) throws Exception {
		ResourceBundle resourceBundle = resourceBundle(portalSession.locale());
		
		// Create new model
		String portletId = (String)event.properties().get(Preferences.PORTLET_ID);
		if (portletId == null) {
			portletId = generatePortletId();
		}
		MarkdownDisplayModel model = putInSession(
				portalSession.browserSession(), 
				new MarkdownDisplayModel(portletId));
		model.setTitle((String)event.properties().getOrDefault(Preferences.TITLE, 
				resourceBundle.getString("portletName")));
		model.setPreviewContent((String)event.properties().getOrDefault(
				Preferences.PREVIEW_SOURCE, ""));
		model.setViewContent((String)event.properties().getOrDefault(
				Preferences.VIEW_SOURCE, ""));
		model.setDeletable((Boolean)event.properties().getOrDefault(
				Preferences.DELETABLE,	Boolean.TRUE));
		@SuppressWarnings("unchecked")
		Set<Principal> editableBy = (Set<Principal>)event.properties().get(
				Preferences.EDITABLE_BY);
		model.setEditableBy(editableBy);
		
		// Save model
		String jsonState = JsonBeanEncoder.create()
				.writeObject(model).toJson();
		portalSession.respond(new KeyValueStoreUpdate().update(
				storagePath(portalSession.browserSession()) + model.getPortletId(),
				jsonState));
		
		// Send HTML
		Set<RenderMode> modes = renderModes(model);
		Template tpl = freemarkerConfig().getTemplate(
				"MarkdownDisplay-preview.ftl.html");
		portalSession.respond(new RenderPortlet(
				MarkdownDisplayPortlet.class, model.getPortletId(),
				templateProcessor(tpl, fmModel(event, portalSession, model)))
				.setRenderMode(DeleteablePreview).setSupportedModes(modes)
				.setForeground(true));
		
		// Fill in data
		updateView(portalSession, model, portalSession.locale());
		return portletId;
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doRenderPortlet(org.jgrapes.portal.events.RenderPortletRequest, org.jgrapes.io.IOSubchannel, org.jgrapes.portal.AbstractPortlet.PortletModelBean)
	 */
	@Override
	protected void doRenderPortlet(RenderPortletRequest event,
	        PortalSession portalSession, String portletId, 
	        Serializable retrievedState) throws Exception {
		MarkdownDisplayModel model = (MarkdownDisplayModel)retrievedState;
		Set<RenderMode> modes = renderModes(model);
		if (model.getViewContent() != null && !model.getViewContent().isEmpty()) {
			modes.add(View);
		}
		switch (event.renderMode()) {
		case Preview:
		case DeleteablePreview: {
			Template tpl = freemarkerConfig().getTemplate("MarkdownDisplay-preview.ftl.html");
			portalSession.respond(new RenderPortlet(
					MarkdownDisplayPortlet.class, model.getPortletId(), 
					templateProcessor(tpl, fmModel(event, portalSession, model)))
					.setRenderMode(event.renderMode()).setSupportedModes(modes)
					.setForeground(event.isForeground()));
			updateView(portalSession, model, portalSession.locale());
			break;
		}
		case View: {
			Template tpl = freemarkerConfig().getTemplate("MarkdownDisplay-view.ftl.html");
			portalSession.respond(new RenderPortlet(
					MarkdownDisplayPortlet.class, model.getPortletId(), 
					templateProcessor(tpl, fmModel(event, portalSession, model)))
					.setRenderMode(View).setSupportedModes(modes)
					.setForeground(event.isForeground()));
			updateView(portalSession, model, portalSession.locale());
			break;
		}
		case Edit: {
			Template tpl = freemarkerConfig().getTemplate("MarkdownDisplay-edit.ftl.html");
			portalSession.respond(new RenderPortlet(
					MarkdownDisplayPortlet.class, model.getPortletId(), 
					templateProcessor(tpl, fmModel(event, portalSession, model)))
					.setRenderMode(Edit).setSupportedModes(modes));
			break;
		}
		default:
			break;
		}
	}

	private Set<RenderMode> renderModes(MarkdownDisplayModel model) {
		Set<RenderMode> modes = new HashSet<>();
		modes.add(model.isDeletable() ? DeleteablePreview : Preview);
		if (model.getViewContent() != null && !model.getViewContent().isEmpty()) {
			modes.add(View);
		}
		if (model.getEditableBy() == null) {
			modes.add(Edit);
		}
		return modes;
	}
	
	private void updateView(IOSubchannel channel, MarkdownDisplayModel model,
	        Locale locale) {
		channel.respond(new NotifyPortletView(type(),
				model.getPortletId(), "updateAll", model.getTitle(), 
				model.getPreviewContent(), model.getViewContent(),
				renderModes(model)));
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doDeletePortlet
	 */
	@Override
	protected void doDeletePortlet(DeletePortletRequest event,
	        PortalSession channel, String portletId, 
	        Serializable retrievedState) throws Exception {
		channel.respond(new KeyValueStoreUpdate().delete(
				storagePath(channel.browserSession()) + portletId));
		channel.respond(new DeletePortlet(portletId));
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.portal.AbstractPortlet#doNotifyPortletModel(org.jgrapes.portal.events.NotifyPortletModel, org.jgrapes.io.IOSubchannel, org.jgrapes.http.Session, java.io.Serializable)
	 */
	@Override
	protected void doNotifyPortletModel(NotifyPortletModel event,
	        PortalSession portalSession, Serializable portletState)
	        throws Exception {
		event.stop();
		Map<Preferences, String> properties = new HashMap<>();
		if (!event.params().isNull(0)) {
			properties.put(Preferences.TITLE, event.params().getString(0));
		}
		if (!event.params().isNull(1)) {
			properties.put(Preferences.PREVIEW_SOURCE, event.params().getString(1));
		}
		if (!event.params().isNull(2)) {
			properties.put(Preferences.VIEW_SOURCE, event.params().getString(2));
		}
		fire(new UpdatePortletModel(event.portletId(), properties), portalSession);
	}

	@SuppressWarnings("unchecked")
	@Handler
	public void onUpdatePortletModel(UpdatePortletModel event, 
			PortalSession portalSession) {
		stateFromSession(portalSession.browserSession(), event.portletId(), 
				MarkdownDisplayModel.class).ifPresent(model -> {
					event.ifPresent(Preferences.TITLE, 
							(key, value) -> model.setTitle((String)value))
					.ifPresent(Preferences.PREVIEW_SOURCE, 
							(key, value) -> model.setPreviewContent((String)value))
					.ifPresent(Preferences.VIEW_SOURCE, 
							(key, value) -> model.setViewContent((String)value))
					.ifPresent(Preferences.DELETABLE, 
							(key, value) -> model.setDeletable((Boolean)value))
					.ifPresent(Preferences.EDITABLE_BY, 
							(key, value) -> {
								model.setEditableBy((Set<Principal>)value);
							});
					String jsonState = JsonBeanEncoder.create()
							.writeObject(model).toJson();
					portalSession.respond(new KeyValueStoreUpdate().update(
							storagePath(portalSession.browserSession()) 
							+ model.getPortletId(),	jsonState));
					updateView(portalSession, model, portalSession.locale());
				});
	}
	
	@SuppressWarnings("serial")
	public static class MarkdownDisplayModel extends PortletBaseModel {

		private String title = "";
		private String previewContent = "";
		private String viewContent = "";
		private boolean deletable = true;
		private Set<Principal> editableBy = null;
		
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

		/**
		 * @return the deletable
		 */
		public boolean isDeletable() {
			return deletable;
		}

		/**
		 * @param deletable the deletable to set
		 */
		public void setDeletable(boolean deletable) {
			this.deletable = deletable;
		}

		/**
		 * @return the editableBy
		 */
		public Set<Principal> getEditableBy() {
			return editableBy;
		}

		/**
		 * @param editableBy the editableBy to set
		 */
		public void setEditableBy(Set<Principal> editableBy) {
			this.editableBy = editableBy;
		}
		
	}
	
}
