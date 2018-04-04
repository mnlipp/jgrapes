/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.http.events;

import java.net.URI;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 * Fired when a protocol upgrade was successful. The result is the
 * new protocol.
 */
public class Upgraded extends Event<String> {

	private final URI resourceName;
	
	/**
	 * @param resourceName the resource for which the upgrade was requested
	 * @param channels
	 */
	public Upgraded(URI resourceName, String result, Channel... channels) {
		super(channels);
		this.resourceName = resourceName;
		setResult(result);
	}

	/**
	 * Return the resource requested in the upgrade.
	 * 
	 * @return the value
	 */
	public URI resourceName() {
		return resourceName;
	}
}
