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

/**
 * An HTTP field value that is an integer.
 * 
 * @author Michael N. Lipp
 */
public class HttpIntField extends HttpField<Long> {

	private long value;
	
	/**
	 * Creates the header field object with the given value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 */
	public HttpIntField(String name, long value) {
		super(name);
		this.value = value;
	}

	protected static <T extends HttpIntField> T fromString
		(Class<T> type, String name, String s) throws ParseException {
		try {
			long value;
			try {
				value = Long.parseLong(unquote(s));
			} catch (NumberFormatException e) {
				throw new ParseException(s, 0);
			}
			T result = type.getConstructor(String.class, long.class)
			        .newInstance(name, value);
			return result;
		} catch (InstantiationException | IllegalAccessException
		        | IllegalArgumentException | InvocationTargetException
		        | NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Creates a new header field object with a value obtained by parsing the
	 * given String.
	 * 
	 * @param name
	 *            the field name
	 * @param s
	 *            the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpIntField fromString(String name, String s)
			throws ParseException {
		return fromString(HttpIntField.class, name, s);
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	@Override
	public Long getValue() {
		return value;
	}
	
	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public int asInt() {
		return (int)value;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asFieldValue() {
		return Long.toString(value);
	}
	
	
}
