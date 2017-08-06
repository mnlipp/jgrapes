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

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.portal.util.JsonUtil;

/**
 * A JSON request from the server to the browser. 
 */
public class JsonOutput extends Event<Void> {

	private JsonObject requestData;
	
	/**
	 * Create a new request from the given data. This constructor provides
	 * complete control of the data to be sent.
	 * 
	 * @param requestData a request as defined by the JSON RPC specification 
	 */
	public JsonOutput(JsonObject requestData) {
		this.requestData = requestData;
	}

	/**
	 * Create a new request that invokes the given method with the
	 * given parameters.
	 * 
	 * @param method the method
	 * @param params the parameters
	 */
	public JsonOutput(String method, Object... params) {
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder notification = factory.createObjectBuilder()
				.add("jsonrpc", "2.0").add("method", method);
		if (params.length > 0) {
			notification.add("params", JsonUtil.toJsonArray(params));
		}
		requestData = notification.build();
	}
	
	/**
	 * @return the requestData
	 */
	public JsonObject requestData() {
		return requestData;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		builder.append("method=");
		builder.append(requestData.getString("method"));
		if (channels != null) {
			builder.append(", channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
