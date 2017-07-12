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

package org.jgrapes.portal.util;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * 
 */
public abstract class JsonUtil {

	private JsonUtil() {
	}

	public static JsonArray toJsonArray(Object... items) {
		return (JsonArray)toJsonValue(items);
	}
	
	public static JsonValue toJsonValue(Object obj) {
		if (obj == null) {
			return JsonValue.NULL;
		}
		if (obj instanceof Boolean) {
			if ((Boolean)obj) {
				return JsonValue.TRUE;
			}
			return JsonValue.FALSE;
		} 
		if (obj instanceof Number) {
			if ((obj instanceof Integer) || (obj instanceof Long)) {
				return Json.createValue((Long)obj);
			} else {
				return Json.createValue((Double)obj);
			}
		} 
		if (obj instanceof Object[]) {
			JsonArrayBuilder builder = Json.createArrayBuilder();
			for (Object o: (Object[])obj) {
				builder.add(toJsonValue(o));
			}
			return builder.build();
		}
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>)obj;
			JsonObjectBuilder builder = Json.createObjectBuilder();
			for (Map.Entry<String, Object> e: map.entrySet()) {
				builder.add(e.getKey(), toJsonValue(e.getValue()));
			}
			return builder.build();
		}
		return Json.createValue(obj.toString());
	}
	
}
