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

	final private String name;
	private String value;
	
	/**
	 * Creates a new representation of a field value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 */
	public HttpField(String name, String value) {
		this.name = name;
		this.value = value.trim();
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
	 * Returns the unparsed value passed to the constructor.
	 * 
	 * @return the value
	 */
	protected String rawValue() {
		return value;
	}

	/**
	 * Returns the header field's value.
	 * 
	 * @return
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
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @return the unquoted value
	 * @throws ParseException 
	 */
	public String unquote() throws ParseException {
		return unquote (value);
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

	public static HttpField<?> parseFieldValue
		(String fieldName, String fieldValue) {
		switch (fieldName.toLowerCase()) {
		case HttpField.CONTENT_TYPE:
			
		}
		return new HttpStringField(fieldName, fieldValue);
	}
	
}
