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

/**
 * Represents a header field that has a simple string as its value.
 * 
 * @author Michael N. Lipp
 */
public class HttpStringField extends HttpField<String> {

	private String value;
	
	/**
	 * Creates a new header field object with the given field name and value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 */
	public HttpStringField(String name, String value) {
		super(name);
		this.value = value.trim();
	}

	/**
	 * Creates a new object with a value obtained by parsing the given
	 * String. Parsing in this case means that the string will be unquoted
	 * if it is quoted.
	 * 
	 * @param name the field name
	 * @param s the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpStringField fromString(String name, String s)
			throws ParseException {
		return new HttpStringField(name, unquote(s.trim()));
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpField#getValue()
	 */
	@Override
	public String getValue() {
		return value;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asFieldValue() {
		return quoteIfNecessary(value);
	}
}
