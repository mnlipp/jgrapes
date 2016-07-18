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

/**
 * A specialization of {@link HttpIntField} that represents the
 * content-length.
 * 
 * @author Michael N. Lipp
 */
public class HttpContentLengthField extends HttpIntField {

	/**
	 * Creates a new content-length field with the given value.
	 * 
	 * @param value the value
	 */
	public HttpContentLengthField(long value) {
		super(CONTENT_LENGTH, value);
	}

	/**
	 * Creates a new object with a value obtained by parsing the given
	 * String.
	 * 
	 * @param s the string to parse
	 * @throws ParseException 
	 */
	public static HttpContentLengthField fromString(String s)
			throws ParseException {
		return fromString(HttpContentLengthField.class, CONTENT_LENGTH, s);
	}
}
