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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
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
import org.jgrapes.portal.events.RenderPortlet;
import org.jgrapes.portal.events.RenderPortletRequest;
import org.jgrapes.portal.events.SetLocale;
import org.jgrapes.portal.events.SetTheme;

/**
 * 
 */
public class Portal extends Component {

	static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
	
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
		try {
			ObjectName mxbeanName = new ObjectName("org.jgrapes.portal:type="
					+ Portal.class.getSimpleName() + "#" + Components.objectId(this));
			mbeanServer.registerMBean(new MBeanView(), mxbeanName);
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException
		        | NotCompliantMBeanException | MalformedObjectNameException e) {
			// Won't happen.
		}
	}

	/**
	 * @return the prefix
	 */
	public URI prefix() {
		return prefix;
	}

	public Portal setResourceBundleSupplier(
			Function<Locale,ResourceBundle> supplier) {
		view.setResourceBundleSupplier(supplier);
		return this;
	}
	
	public Portal setFallbackResourceSupplier(
			BiFunction<ThemeProvider,String,InputStream> supplier) {
		view.setFallbackResourceSupplier(supplier);
		return this;
	}
	
	@Handler
	public void onRenderPortlet(RenderPortlet event, IOSubchannel channel) 
			throws InterruptedException, IOException {
		StringWriter content = new StringWriter();
		CharBuffer buffer = CharBuffer.allocate(8192);
		try (Reader in = new BufferedReader(event.contentReader())) {
			while (true) {
				if (in.read(buffer) < 0) {
					break;
				}
				buffer.flip();
				content.append(buffer);
				buffer.clear();
			}
		}
		fire(new JsonOutput("updatePortlet",
				event.portletId(), event.renderMode().name(),
				event.supportedRenderModes().stream().map(RenderMode::name)
				.toArray(size -> new String[size]),
				content.toString(), event.isForeground()), channel);
	}
	
	@Handler
	public void onAddPortletType(AddPortletType event, IOSubchannel channel)
			throws InterruptedException, IOException {
		fire(new JsonOutput("addPortletType",
				event.portletType(),
				event.displayName(),
				Arrays.stream(event.cssUris()).map(uri -> 
					view.renderSupport().portletResource(
							event.portletType(), uri).toString())
				.toArray(String[]::new),
				Arrays.stream(event.scriptUris()).map(uri -> 
					view.renderSupport().portletResource(
							event.portletType(), uri).toString())
				.toArray(String[]::new),
				event.isInstantiable()), channel);
	}
	
	@Handler
	public void onDeletePortlet(DeletePortlet event, IOSubchannel channel) 
					throws InterruptedException, IOException {
		fire(new JsonOutput("deletePortlet", event.portletId()), channel);
	}
	
	@Handler 
	public void onNotifyPortletView(
			NotifyPortletView event, IOSubchannel channel) 
					throws InterruptedException, IOException {
		fire(new JsonOutput("invokePortletMethod",
				event.portletClass(), event.portletId(), 
				event.method(), event.params()), channel);
	}

	@Handler
	public void onJsonInput(JsonInput event, IOSubchannel channel) 
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
			List<List<String>> previewLayout = params.getJsonArray(0)
					.getValuesAs(column -> ((JsonArray)column)
							.getValuesAs(JsonString::getString)
							.stream().collect(Collectors.toList()))
					.stream().collect(Collectors.toList());
			List<String> tabsLayout = params.getJsonArray(1)
					.getValuesAs(JsonString::getString)
					.stream().collect(Collectors.toList());
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
			PortalConfigured event, IOSubchannel channel) 
					throws InterruptedException, IOException {
		fire(new JsonOutput("portalConfigured"), channel);
	}
	
	@Handler
	public void onLastPortalLayout(
			LastPortalLayout event, IOSubchannel channel) 
					throws InterruptedException, IOException {
		fire(new JsonOutput("lastPortalLayout",
				event.previewLayout(), event.tabsLayout()), channel);
	}
	
	/**
	 * An MBean interface for the portal component.
	 */
	public static interface ManagedPortalMXBean {
		
		/**
		 * Indicates if minified resources are sent to the browser.
		 * 
		 * @return the result
		 */
		boolean getUseMinifiedResources();
		
		/**
		 * Determines if minified resources are sent to the browser.
		 * 
		 * @param useMinified
		 */
		void setUseMinifiedResources(boolean useMinified);
	}
	
	private class MBeanView implements ManagedPortalMXBean {

		/* (non-Javadoc)
		 * @see org.jgrapes.portal.Portal.ManagedPortalMXBean#getUseMinifiedResources()
		 */
		@Override
		public boolean getUseMinifiedResources() {
			return view.useMinifiedResources();
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.portal.Portal.ManagedPortalMXBean#setUseMinifiedResources(boolean)
		 */
		@Override
		public void setUseMinifiedResources(boolean useMinifiedResources) {
			view.setUseMinifiedResources(useMinifiedResources);
		}
		
	}
}
