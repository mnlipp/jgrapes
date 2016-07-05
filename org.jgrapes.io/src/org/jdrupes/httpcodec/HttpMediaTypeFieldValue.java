/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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
package org.jdrupes.httpcodec;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdrupes.httpcodec.util.HttpUtils;

/**
 * Represents a header field's value and provides methods for interpreting
 * that value.
 * 
 * @author Michael N. Lipp
 */
public class HttpMediaTypeFieldValue extends HttpFieldValue {

	private String type;
	private String subtype;
	private Map<String, String> parameters;
	
	/**
	 * Creates a new representation of a media type field value.
	 * 
	 * @param value
	 * @throws ParseException 
	 */
	public HttpMediaTypeFieldValue(String value) throws ParseException {
		super(value);
		String[] parts = value.split(";");
		if (parts.length == 0) {
			throw new ParseException(value, 0);
		}
		String[] typeSubtype = parts[0].trim().split("/");
		if (typeSubtype.length != 2) {
			throw new ParseException(value, 0);
		}
		type = typeSubtype[0].toLowerCase();
		subtype = typeSubtype[1].toLowerCase();
		
		parameters = HttpUtils.caseInsensitiveMap(new HashMap<>());
		for (int i = 1; i < parts.length; i++) {
			String[] paramParts = parts[i].split("=");
			if (paramParts.length != 2) {
				throw new ParseException(value, value.indexOf(parts[i]));
			}
			parameters.put(paramParts[0].trim(), 
					unquote(paramParts[1].trim()).toLowerCase());
		}
	}

	/**
	 * Creates new object with the given type and subtype and no parameters.
	 * 
	 * @param type the type 
	 * @param subtype the subtype
	 * @throws ParseException 
	 */
	public HttpMediaTypeFieldValue(String type, String subtype) 
			throws ParseException {
		this(type + "/" + subtype);
	}



	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asString() {
		StringBuilder result = new StringBuilder();
		result.append(type);
		result.append('/');
		result.append(subtype);
		for (Map.Entry<String, String> e: parameters.entrySet()) {
			result.append(";");
			result.append(e.getKey());
			result.append('=');
			result.append(quoteIfNecessary(e.getValue()));
		}
		return result.toString();
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the subtype
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * Returns the parameters as unmodifiable map.
	 * 
	 * @return the parameters
	 */
	public Map<String, String> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	/**
	 * Sets the parameter with the given name to the given value.
	 * 
	 * @param name the name
	 * @param value the value
	 */
	public void setParameter(String name, String value) {
		parameters.put(name, value.toLowerCase());
	}

	/**
	 * Returns the value of the parameter with the given name or
	 * {@code null} if the parameter is undefined.
	 * 
	 * @param name the name
	 * @return the value or {@code null}
	 */
	public String getParameter(String name) {
		return parameters.get(name);
	}
}
