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

package org.jgrapes.http.demo.httpserver;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.portal.PortalView;
import org.jgrapes.portal.events.AddPortletResources;
import org.jgrapes.portal.events.PortalReady;
import org.jgrapes.portal.events.PortletResourceRequest;
import org.jgrapes.portal.events.RenderPortletFromString;
import org.jgrapes.portal.events.RenderPortletRequest;

import static org.jgrapes.portal.Portlet.*;

import java.io.InputStream;

import org.jdrupes.httpcodec.protocols.http.HttpResponse;

/**
 * 
 */
public class HelloWorldPortlet extends Component {

	private String portletId;
	
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
		portletId = Components.objectId(this);
	}

	@Handler
	public void onPortalReady(PortalReady event, IOSubchannel channel) {
		channel.respond(new AddPortletResources(getClass().getName())
				.addScript(PortalView.uriFromPath("functions.js")));
		String html = "<div>Hello World! <img style='vertical-align: bottom;'"
				+ " src='" + event.renderSupport().portletResource(
						getClass().getName(), 
						PortalView.uriFromPath("globe#1.ico")) 
				+ "' height='20'></div>";
		channel.respond(new RenderPortletFromString(
				portletId, "Hello World", RenderMode.Preview, 
				VIEWABLE_PORTLET_MODES, html));
	}
	
	@Handler
	public void onResourceRequest(
			PortletResourceRequest event, IOSubchannel channel) {
		// For me?
		if (!event.portletType().equals(getClass().getName())) {
			return;
		}
		
		// Look for content
		InputStream in = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (in == null) {
			return;
		}
		
		// Send header
		HttpResponse response = event.httpRequest().response().get();
		PortalView.prepareResourceResponse(response, event.resourceUri());
		event.httpChannel().respond(new Response(response));
		
		// Send content
		activeEventPipeline().executorService()
			.submit(new InputStreamPipeline(in, event.httpChannel()));
		
		// Done
		event.setResult(true);
	}
	
	@Handler
	public void onRenderPortletRequest(RenderPortletRequest event,
			IOSubchannel channel) {
		if (!event.portletId().equals(getClass().getName())) {
			return;
		}
		String html = "<div><h1>Hello World!</h1></div>";
		channel.respond(new RenderPortletFromString(
				portletId, "Hello World", RenderMode.View, 
				VIEWABLE_PORTLET_MODES, html));
		event.stop();
	}
}
