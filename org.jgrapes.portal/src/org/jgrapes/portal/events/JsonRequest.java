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

import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * 
 */
public class JsonRequest extends Event<Void> {

	private String method;
	private JsonStructure params;
	private Optional<JsonValue> id;
	
	/**
	 */
	public JsonRequest(JsonObject requestData) {
		method = requestData.getString("method");
		params = (JsonStructure)requestData.get("params");
		id = Optional.ofNullable(requestData.get("id"));
	}

	/**
	 * @return the method
	 */
	public String method() {
		return method;
	}

	/**
	 * @return the params
	 */
	public JsonStructure params() {
		return params;
	}

	/**
	 * @return the id
	 */
	public Optional<JsonValue> id() {
		return id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (method != null) {
			builder.append("method=");
			builder.append(method);
			builder.append(", ");
		}
		if (id.isPresent()) {
			builder.append("id=");
			builder.append(id.get());
		}
		if (channels != null) {
			builder.append("channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
