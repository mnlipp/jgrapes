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

import java.net.HttpCookie;
import java.text.ParseException;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpCookieListField extends HttpListField<HttpCookie> {

	/**
	 * Creates a new object with the field name "Cookie" and the given unparsed
	 * value.
	 * 
	 * @param s the unparsed value
	 */
	protected HttpCookieListField(String s) {
		super(HttpField.COOKIE, s);
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param s the string to parse
	 * @throws ParseException 
	 */
	public static HttpCookieListField fromString(String s) 
			throws ParseException {
		HttpCookieListField result = new HttpCookieListField(s);
		while (true) {
			String element = result.nextElement();
			if (element == null) {
				break;
			}
			try {
				result.addAll(HttpCookie.parse(element));
			} catch (IllegalArgumentException e) {
				throw new ParseException(element, 0);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpListField#getSeparator()
	 */
	@Override
	protected char getDelimiter() {
		return ';';
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpListField#elementToString(java.lang.Object)
	 */
	@Override
	protected String elementToString(HttpCookie element) {
		return element.toString();
	}

	/**
	 * Returns the value for the cookie with the given name.
	 * 
	 * @param name the name
	 * @return the value or {@code null} if no cookie with the given name exists
	 */
	public String valueForName(String name) {
		for (HttpCookie cookie: this) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
