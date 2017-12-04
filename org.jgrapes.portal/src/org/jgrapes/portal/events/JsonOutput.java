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

import java.io.Writer;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.jdrupes.json.JsonBeanEncoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * A JSON request from the server to the browser. 
 */
public class JsonOutput extends Event<Void> {

	private String method;
	private Object[] params;
	
	/**
	 * Create a new request that invokes the given method with the
	 * given parameters.
	 * 
	 * @param method the method
	 * @param params the parameters
	 */
	public JsonOutput(String method, Object... params) {
		this.method = method;
		this.params = params;
	}
	
	/**
	 * Writes the event's JSON data to th egiven writer.
	 * 
	 * @param writer the writer
	 */
	public void toJson(Writer writer) {
		JsonGenerator generator = Json.createGenerator(writer);
		generator.writeStartObject();
		generator.write("jsonrpc", "2.0");
		generator.write("method", method);
		if (params.length > 0) {
			generator.writeKey("params");
			generator.writeStartArray();
			for (Object obj: params) {
				if (obj == null) {
					generator.writeNull();
					continue;
				}
				if (obj instanceof JsonValue) {
					generator.write((JsonValue)obj);
					continue;
				}
				JsonBeanEncoder.create(generator).writeObject(obj);
			}
			generator.writeEnd();
		}
		generator.writeEnd();
		generator.flush();
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
		builder.append(method);
		if (channels != null) {
			builder.append(", channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
	}
}
