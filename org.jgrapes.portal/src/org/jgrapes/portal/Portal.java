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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.events.AddPortletType;
import org.jgrapes.portal.events.DeletePortlet;
import org.jgrapes.portal.events.NotifyPortletView;
import org.jgrapes.portal.events.PortletResourceResponse;
import org.jgrapes.portal.events.RenderPortletFromProvider;
import org.jgrapes.portal.events.RenderPortletFromString;

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

}
