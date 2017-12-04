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

package org.jgrapes.portal.events;

import java.net.URI;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Event;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.portal.RenderSupport;

/**
 * An event that signals the request of a resource that is to be added
 * to the `<HEAD>` section of the page.
 *
 * Requests for such resources are usually
 * generated during portal boot. See the description of
 * {@link AddPageResources} for details.
 */
public class PageResourceRequest extends Event<Boolean> {

	private HttpRequest httpRequest;
	private IOSubchannel httpChannel;
	private URI resourceUri;
	private RenderSupport renderSupport;

	/**
	 * Creates a new request.
	 * 
	 * @param resourceUri the requested resource
	 * @param httpRequest the original HTTP request
	 * @param httpChannel the channel that the HTTP request was received on
	 * @param renderSupport the render support
	 */
	public PageResourceRequest(URI resourceUri,
			HttpRequest httpRequest, IOSubchannel httpChannel,
			RenderSupport renderSupport) {
		this.resourceUri = resourceUri;
		this.httpRequest = httpRequest;
		this.httpChannel = httpChannel;
		this.renderSupport = renderSupport;
	}

	/**
	 * Returns the "raw" request as provided by the HTTP decoder.
	 * 
	 * @return the request
	 */
	public HttpRequest httpRequest() {
		return httpRequest;
	}

	/**
	 * @return the httpChannel
	 */
	public IOSubchannel httpChannel() {
		return httpChannel;
	}

	/**
	 * @return the resourceUri
	 */
	public URI resourceUri() {
		return resourceUri;
	}
	
	/**
	 * Returns the render support.
	 * 
	 * @return the render support
	 */
	public RenderSupport renderSupport() {
		return renderSupport;
	}
}
