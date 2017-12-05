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

import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.util.InputStreamPipeline;
import org.jgrapes.portal.events.PageResourceRequest;

/**
 * Base class for implementing components that add resources to
 * the `<HEAD>` section of the portal page.
 */
public abstract class PageResourceProvider extends Component {	

	/**
	 * Creates a new component.
	 * 
	 * @param channel the channel to listen on
	 */
	public PageResourceProvider(Channel channel) {
		super(channel);
	}

	/**
	 * Provides a resource bundle for localization.
	 * The default implementation looks up a bundle using the
	 * package name plus "l10n" as base name.
	 * 
	 * @return the resource bundle
	 */
	protected ResourceBundle resourceBundle(Locale locale) {
		return ResourceBundle.getBundle(
			getClass().getPackage().getName() + ".l10n", locale, 
			getClass().getClassLoader(),
				ResourceBundle.Control.getNoFallbackControl(
						ResourceBundle.Control.FORMAT_DEFAULT));
	}

	/**
	 * A default handler for resource requests. searches for
	 * a file with the requested URI in the component's class path
	 * and sends its content if found.
	 * 
	 * @param event the resource request event
	 * @param channel the channel that the request was received on
	 */
	@Handler
	public final void onResourceRequest(
			PageResourceRequest event, IOSubchannel channel) {
		InputStream stream = this.getClass().getResourceAsStream(
				event.resourceUri().getPath());
		if (stream == null) {
			return;
		}
		
		// Resource found, send.
		// Send header
		HttpResponse response = event.httpRequest().response().get();
		PortalView.prepareResourceResponse(
				response, event.httpRequest().requestUri(), false);
		event.httpChannel().respond(new Response(response));
		
		// Send content
		event.httpChannel().responsePipeline().executorService()
			.submit(new InputStreamPipeline(stream, event.httpChannel()));
		
		// Done
		event.setResult(true);
		event.stop();
	}
	
}
