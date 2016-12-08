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

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * The HTTP Date field.
 * 
 * @author Michael N. Lipp
 */
public class HttpDateField extends HttpField<Instant> {

	private Instant value;

	/**
	 * Creates a header field object with the given value.
	 * 
	 * @param name the field name
	 * @param value
	 *            the field value
	 */
	public HttpDateField(String name, Instant value) {
		super(name);
		this.value = value;
	}

	/**
	 * Creates a header field object with name "Date" and the current Time.
	 */
	public HttpDateField() {
		this(HttpField.DATE, Instant.now());
	}

	protected static <T extends HttpDateField> T fromString(Class<T> type,
	        String name, String s) throws ParseException {
		try {
			T result = type.getConstructor(String.class, Instant.class)
			        .newInstance(name, Instant.EPOCH);
			try {
				((HttpDateField) result).value = Instant
				        .from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
			} catch (DateTimeParseException e) {
				throw new ParseException(s, 0);
			}
			return result;
		} catch (InstantiationException | IllegalAccessException
		        | IllegalArgumentException | InvocationTargetException
		        | NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Creates a new object with a value obtained by parsing the given String.
	 * 
	 * @param name
	 *            the field name
	 * @param s
	 *            the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpDateField fromString(String name, String s)
	        throws ParseException {
		return fromString(HttpDateField.class, name, s);
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	@Override
	public Instant getValue() {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asFieldValue() {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format
				(value.atZone(ZoneId.of("GMT")));	
	}

}
