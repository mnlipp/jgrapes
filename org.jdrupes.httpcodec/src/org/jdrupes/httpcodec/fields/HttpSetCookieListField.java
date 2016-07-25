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
 * Represents all "Set-Cookie" fields in a Response header. Several
 * cookies are actually set with several headers. However, to provide
 * uniform access to all header fields, they are converted to a field
 * with a list of values in the internal representation.
 * 
 * @author Michael N. Lipp
 */
public class HttpSetCookieListField extends HttpListField<HttpCookie> {

	/**
	 * Creates a new header field object with the field name "Set-Cookie".
	 */
	public HttpSetCookieListField() {
		super(HttpField.SET_COOKIE);
	}

	/**
	 * Adds a new cookie obtained by parsing the given String.
	 * 
	 * @param s the string to parse
	 * @return this object for easy chaining
	 * @throws ParseException 
	 */
	public HttpSetCookieListField addFromString(String s)
	        throws ParseException {
		try {
			addAll(HttpCookie.parse(s));
		} catch (IllegalArgumentException e) {
			throw new ParseException(s, 0);
		}
		return this;
	}

	/**
	 * Creates a new object and adds the set-cookie obtained by parsing the
	 * given String.
	 * 
	 * @param s
	 *            the string to parse
	 * @return this object for easy chaining
	 * @throws ParseException
	 */
	public static HttpSetCookieListField fromString(String s)
	        throws ParseException {
		HttpSetCookieListField result = new HttpSetCookieListField();
		result.addFromString(s);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpListField#elementToString(java.lang.Object)
	 */
	@Override
	protected String elementToString(HttpCookie element) {
		throw new UnsupportedOperationException();
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
