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

package org.jgrapes.http.rocker.events;

import com.fizzed.rocker.RockerModel;

import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 * 
 */
public class Render extends Event<Void> {

	private HttpRequest request;
	private RockerModel model;
	
	/**
	 * @param channels
	 */
	public Render(HttpRequest request, RockerModel model, Channel... channels) {
		super(channels);
		this.request = request;
		this.model = model;
	}

	/**
	 * @return the request
	 */
	public HttpRequest request() {
		return request;
	}

	/**
	 * @return the model
	 */
	public RockerModel model() {
		return model;
	}
	
}
