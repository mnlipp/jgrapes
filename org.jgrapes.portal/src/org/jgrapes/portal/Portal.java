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

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.json.JsonArray;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.Portlet.RenderMode;
import org.jgrapes.portal.events.AddPortletRequest;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.DeletePortletRequest;
import org.jgrapes.portal.events.JsonInput;
import org.jgrapes.portal.events.JsonOutput;
import org.jgrapes.portal.events.LastPortalLayout;
import org.jgrapes.portal.events.NotifyPortletModel;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortalConfigured;
import org.jgrapes.portal.events.PortalLayoutChanged;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletFromProvider;
import org.jgrapes.portal.events.RenderPortletFromString;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.events.SetLocale;
import org.jgrapes.portal.events.SetTheme;

/**
 * 
 */
public class Portal extends Component {

	private URI prefix;
	private PortalView view;
	
	/**
	 * 
	 */
	public Portal(URI prefix) {
		this(Channel.SELF, prefix);
	}

	/**
	 * @param componentChannel
	 */
	public Portal(Channel componentChannel, URI prefix) {
		this(componentChannel, componentChannel, prefix);
	}

	/**
	 * @param componentChannel
	 */
	public Portal(Channel componentChannel, Channel viewChannel, URI prefix) {
		super(componentChannel);
		this.prefix = URI.create(prefix.getPath().endsWith("/") 
				? prefix.getPath() : (prefix.getPath() + "/"));
		view = attach(new PortalView(this, viewChannel));
	}

	/**
	 * @return the prefix
	 */
	public URI prefix() {
		return prefix;
	}

	public Portal setResourceSupplier(
			Function<Locale,ResourceBundle> supplier) {
		view.setResourceSupplier(supplier);
		return this;
	}
	
	@Handler
	public void onRenderPortlet(RenderPortletFromString result,
			LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		view.onRenderPortlet(result, channel);
	}
	
	@Handler
	public void onRenderPortlet(RenderPortletFromProvider result,
			LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		view.onRenderPortlet(result, channel);
	}
	
	@Handler
	public void onAddPortletResources(
			AddPortletType event, LinkedIOSubchannel channel)
					throws InterruptedException, IOException {
		view.onAddPortletType(event, channel);
	}
	
	@Handler
	public void onDeletePortlet(
			DeletePortlet event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		view.onDeletePortlet(event, channel);
	}
	
	@Handler
	public void onPortletResourceResponse(
			PortletResourceResponse event, LinkedIOSubchannel channel) {
		view.onPortletResourceResponse(event, channel);
	}
	
	@Handler 
	public void onNotifyPortletView(
			NotifyPortletView event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		view.onNotifyPortletView(event, channel);
	}

	@Handler
	public void onJsonInput(JsonInput event, LinkedIOSubchannel channel) 
			throws InterruptedException, IOException {
		// Send events to portlets on portal's channel
		JsonArray params = (JsonArray)event.params();
		switch (event.method()) {
		case "portalReady": {
			fire(new PortalReady(view.renderSupport()), channel);
			break;
		}
		case "addPortlet": {
			fire(new AddPortletRequest(view.renderSupport(), params.getString(0),
					RenderMode.valueOf(params.getString(1))), channel);
			break;
		}
		case "deletePortlet": {
			fire(new DeletePortletRequest(
					view.renderSupport(), params.getString(0)), channel);
			break;
		}
		case "portalLayout": {
			String[][] previewLayout = params.getJsonArray(0)
					.getValuesAs(column -> ((JsonArray)column)
							.getValuesAs(JsonString::getString)
							.stream().toArray(s -> new String[s]))
					.stream().toArray(s -> new String[s][]);
			String[] tabsLayout = params.getJsonArray(1)
					.getValuesAs(JsonString::getString)
					.stream().toArray(s -> new String[s]);
			fire(new PortalLayoutChanged(
					previewLayout, tabsLayout), channel);
			break;
		}
		case "renderPortlet": {
			fire(new RenderPortletRequest(view.renderSupport(), params.getString(0),
					RenderMode.valueOf(params.getString(1)),
					params.getBoolean(2)), channel);
			break;
		}
		case "setLocale": {
			fire(new SetLocale(Locale.forLanguageTag(params.getString(0))),
					channel);
			break;
		}
		case "setTheme": {
			fire(new SetTheme(params.getString(0)), channel);
			break;
		}
		case "sendToPortlet": {
			fire(new NotifyPortletModel(view.renderSupport(), params.getString(0),
					params.getString(1), params.size() <= 2
					? JsonValue.EMPTY_JSON_ARRAY : params.getJsonArray(2)),
					channel);
			break;
		}
		}		
	}
	
	@Handler
	public void onPortalConfigured(
			PortalConfigured event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		fire(new JsonOutput("portalConfigured"), channel);
	}
	
	@Handler
	public void onLastPortalLayout(
			LastPortalLayout event, LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		fire(new JsonOutput("lastPortalLayout",
				event.previewLayout(), event.tabsLayout()), channel);
	}
	
}
