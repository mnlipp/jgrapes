/*******************************************************************************
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.jdrupes.httpcodec.protocols.http.fields;

import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

import org.jdrupes.httpcodec.protocols.http.HttpConstants;

/**
 * A base class for all kinds of header field values.
 * 
 * @author Michael N. Lipp
 */
public abstract class HttpField<T> implements Cloneable {

	final public static String COOKIE = "Cookie";
	final public static String CONNECTION = "Connection";
	final public static String CONTENT_LENGTH = "Content-Length";
	final public static String CONTENT_TYPE = "Content-Type";
	final public static String DATE = "Date";
	final public static String HOST = "Host";
	final public static String SET_COOKIE = "Set-Cookie";
	final public static String TE = "TE";
	final public static String TRAILER = "Trailer";
	final public static String TRANSFER_ENCODING = "Transfer-Encoding";
	final public static String UPGRADE = "Upgrade";
	final public static String VIA = "Via";
	
	private static Map<String, String> fieldNameMap = new TreeMap<>(
	        String.CASE_INSENSITIVE_ORDER);
	static {
		fieldNameMap.put(COOKIE, COOKIE);
		fieldNameMap.put(CONNECTION, CONNECTION);
		fieldNameMap.put(CONTENT_LENGTH, CONTENT_LENGTH);
		fieldNameMap.put(CONTENT_TYPE, CONTENT_TYPE);
		fieldNameMap.put(DATE, DATE);
		fieldNameMap.put(HOST, HOST);
		fieldNameMap.put(SET_COOKIE, SET_COOKIE);
		fieldNameMap.put(TE, TE);
		fieldNameMap.put(TRAILER, TRAILER);
		fieldNameMap.put(TRANSFER_ENCODING, TRANSFER_ENCODING);
		fieldNameMap.put(UPGRADE, UPGRADE);
		fieldNameMap.put(VIA, VIA);
	}
	
	/**
	 * Returns an HttpField that represents the given header field, using the
	 * best matching derived class in this package. Works for all well known
	 * field names, i.e. the field names defined as constants in this class. If
	 * the field name is unknown, the result will be of type
	 * {@link HttpStringField}.
	 * 
	 * @param fieldName
	 *            the field name
	 * @param fieldValue
	 *            the field value
	 * @return a typed representation
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpField<?> fromString(String fieldName,
	        String fieldValue) throws ParseException {
		String normalizedFieldName = fieldNameMap
				.getOrDefault(fieldName, fieldName);
		switch (normalizedFieldName) {
		case HttpField.COOKIE:
			return HttpCookieListField.fromString(fieldValue);
		case HttpField.CONNECTION:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.CONTENT_LENGTH:
			return HttpContentLengthField.fromString(fieldValue);
		case HttpField.CONTENT_TYPE:
			return HttpMediaTypeField.fromString(fieldName, fieldValue);
		case HttpField.DATE:
			return HttpDateField.fromString(fieldName, fieldValue);
		case HttpField.SET_COOKIE:
			return HttpSetCookieListField.fromString(fieldValue);
		case HttpField.TRAILER:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.TRANSFER_ENCODING:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.UPGRADE:
			return HttpStringListField.fromString(fieldName, fieldValue);
		case HttpField.VIA:
			return HttpStringListField.fromString(fieldName, fieldValue);
		default:
			return HttpStringField.fromString(fieldName, fieldValue);
		}
	}
	
	final private String name;
	
	/**
	 * Creates a new representation of a header field value. For fields with
	 * a constant definition in this class, the name is normalized.
	 * 
	 * @param name the field name
	 */
	protected HttpField(String name) {
		this.name = fieldNameMap.getOrDefault(name, name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public HttpField<T> clone() {
		try {
			return (HttpField<T>)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Returns the header field name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the header field's parsed value.
	 * 
	 * @return the field's value
	 */
	public abstract T getValue();
	
	/**
	 * Returns the string representation of this field's value.
	 * 
	 * @return the field value as string
	 */
	public abstract String asFieldValue();
	
	/**
	 * Returns the string representation of this header field as it appears in
	 * an HTTP message. Note that the returned string may span several
	 * lines (may contain CRLF).
	 * 
	 * @return the field as it occurs in a header
	 */
	public String asHeaderField() {
		return getName() + ": " + asFieldValue();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(getClass().getSimpleName());
		s.append(" [");
		s.append(asHeaderField().replace("\r\n", " CRLF "));
		s.append("]");
		return s.toString();
	}

	/**
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @param value the value to unquote
	 * @return the unquoted value
	 * @throws ParseException if the input violates the field format
	 */
	public static String unquote(String value) throws ParseException {
		// RFC 7230 3.2.6
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
		// RFC 7230 3.2.6
		StringBuilder result = new StringBuilder();
		int position = 0;
		boolean needsQuoting = false;
		result.append('"');
		while (position < value.length()) {
			char ch = value.charAt(position++);
			if (!needsQuoting && HttpConstants.TOKEN_CHARS.indexOf(ch) < 0) {
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
