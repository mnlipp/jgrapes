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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.portal.events.RenderPortlet;

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

	@Handler
	public void onRenderPortletResult(RenderPortlet result,
			LinkedIOSubchannel channel) 
					throws InterruptedException, IOException {
		view.renderPortletResult(result, channel);
	}
}
