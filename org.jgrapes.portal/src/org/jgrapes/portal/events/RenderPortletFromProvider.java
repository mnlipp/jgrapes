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

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.jgrapes.portal.Portlet.RenderMode;

/**
 * Send to the portal for adding or updating a portlet. The content
 * is provided by a method that writes to a {@link Writer}.
 */
public class RenderPortletFromProvider extends RenderPortlet {

	private ContentProvider provider;
	
	/**
	 * Creates a new event.
	 * 
	 * @param portletId the id of the portlet
	 * @param mode the view mode that is to be updated
	 * @param supportedModes the modes supported by the portlet
	 * @param provider the content provider
	 */
	public RenderPortletFromProvider(
			String portletId, RenderMode mode, Set<RenderMode> supportedModes,
			ContentProvider provider) {
		super(portletId, mode, supportedModes);
		this.provider = provider;
	}

	/**
	 * Returns the provider.
	 * 
	 * @return the provider
	 */
	public ContentProvider provider() {
		return provider;
	}

	/**
	 * Implemented by content providers. The only requirement for
	 * content providers is that they can stream the HTML that
	 * defines the portlet view.
	 */
	public interface ContentProvider {

		/**
		 * Writes the content to the goven stream.
		 * 
		 * @param out the (character based) output stream
		 * @throws IOException
		 */
		void writeTo(Writer out) throws IOException;
	}
}
