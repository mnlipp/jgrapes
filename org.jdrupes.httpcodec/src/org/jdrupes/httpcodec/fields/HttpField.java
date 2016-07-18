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
package org.jdrupes.httpcodec.fields;

import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

import org.jdrupes.httpcodec.util.HttpUtils;

/**
 * A base class for all kinds of field values.
 * 
 * @author Michael N. Lipp
 */
public abstract class HttpField<T> {

	final public static String CONNECTION = "Connection";
	final public static String CONTENT_LENGTH = "Content-Length";
	final public static String CONTENT_TYPE = "Content-Type";
	final public static String HOST = "Host";
	final public static String TRANSFER_ENCODING = "Transfer-Encoding";

	private static Map<String, String> fieldNameMap = new TreeMap<>(
	        String.CASE_INSENSITIVE_ORDER);
	static {
		fieldNameMap.put(CONNECTION, CONNECTION);
		fieldNameMap.put(CONTENT_LENGTH, CONTENT_LENGTH);
		fieldNameMap.put(CONTENT_TYPE, CONTENT_TYPE);
		fieldNameMap.put(HOST, HOST);
		fieldNameMap.put(TRANSFER_ENCODING, TRANSFER_ENCODING);
	}
	
	/**
	 * Returns an HttpField that represents the given header field,
	 * using the best matching derived class in this package. Works
	 * for all well known field names, i.e. the field names defined
	 * as constants in this class. If the field name is unknown,
	 * the result will be of type {@link HttpStringField}.
	 * 
	 * @param fieldName the field name
	 * @param fieldValue the field value
	 * @return a typed representation
	 * @throws ParseException
	 */
	public static HttpField<?> fromString(String fieldName,
	        String fieldValue) throws ParseException {
		String normalizedFieldName = fieldNameMap
				.getOrDefault(fieldName, fieldName);
		switch (normalizedFieldName) {
		case HttpField.CONNECTION:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.CONTENT_LENGTH:
			return HttpContentLengthField.fromString(fieldName, fieldValue);
		case HttpField.CONTENT_TYPE:
			return HttpMediaTypeField.fromString(fieldName, fieldValue);
		case HttpField.TRANSFER_ENCODING:
			return HttpStringListField.fromString(fieldName, fieldValue);
		default:
			return HttpStringField.fromString(fieldName, fieldValue);
		}
	}
	
	final private String name;
	
	/**
	 * Creates a new representation of a field value. For fields with
	 * a constant definition in this class, the name is normalized.
	 * 
	 * @param name the field name
	 */
	protected HttpField(String name) {
		this.name = fieldNameMap.getOrDefault(name, name);
	}

	/**
	 * Returns the field name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the header field's value.
	 * 
	 * @return the field's value
	 */
	public abstract T getValue();
	
	/**
	 * Returns the string representation of this field's value.
	 * 
	 * @return the field value as string
	 */
	public abstract String valueToString();
	
	/**
	 * Returns the string representation of this field.
	 * 
	 * @return the field as it occurs in a header
	 */
	public String toString() {
		return getName() + ": " + valueToString();
	}
	
	/**
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @param value the value to unquote
	 * @return the unquoted value
	 * @throws ParseException 
	 */
	public static String unquote(String value) throws ParseException {
		if (value.length() == 0 || value.charAt(0) != '\"') {
			return value;
		}
		int startPosition = 1;
		int position = 1;
		try {
			StringBuilder result = new StringBuilder();
			while (true) {
				char ch = value.charAt(position);
				switch (ch) {
				case '\\':
					result.append(value.substring(startPosition, position));
					position += 1;
					result.append(value.charAt(position));
					position += 1;
					startPosition = position;
					continue;
				case '\"':
					if (position != value.length() - 1) {
						throw new ParseException(value, position);
					}
					result.append(value.substring(startPosition, position));
					return result.toString();
				default:
					position += 1;
					continue;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(value, position);
		}
	}

	/**
	 * Returns the given string as double quoted string if necessary.
	 * 
	 * @param value the value to quote if necessary
	 * @return the result
	 */
	public static String quoteIfNecessary(String value) {
		StringBuilder result = new StringBuilder();
		int position = 0;
		boolean needsQuoting = false;
		result.append('"');
		while (position < value.length()) {
			char ch = value.charAt(position++);
			if (!needsQuoting && HttpUtils.TCHARS.indexOf(ch) < 0) {
				needsQuoting = true;
			}
			switch(ch) {
			case '"':
			case '\\':
				result.append('\\');
			default:
				result.append(ch);
			}
		}
		result.append('\"');
		if (needsQuoting) {
			return result.toString();
		}
		return value;
	}

}
