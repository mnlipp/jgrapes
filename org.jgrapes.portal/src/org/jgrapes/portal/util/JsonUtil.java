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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;

/**
 * 
 */
public abstract class JsonUtil {

	private JsonUtil() {
	}

	public static JsonArray toJsonArray(Object... items) {
		return toJsonArray(Json.createBuilderFactory(null), items);
	}
	
	public static JsonArray toJsonArray(
			JsonBuilderFactory factory, Object... items) {
		return toJsonArray(factory, null, items).build();
	}
	
	private static JsonArrayBuilder toJsonArray(JsonBuilderFactory factory,
			JsonArrayBuilder array, Object item) {
		if (item instanceof Object[]) {
			JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
			for (Object nested: (Object[])item) {
				toJsonArray(factory, arrayBuilder, nested);
			}
			if (array == null) {
				return arrayBuilder;
			}
			array.add(arrayBuilder);
			return array;
		}
		if (array == null) {
			array = factory.createArrayBuilder();
		}
		if (item instanceof Boolean) {
			array.add((Boolean)item);
		} else if (item instanceof Number) {
			if ((item instanceof Integer) || (item instanceof Long)) {
				array.add((Long)item);
			} else {
				array.add((Double)item);
			}
		} else {
			array.add(item.toString());
		}
		return array;
	}

	
}
