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

import java.net.HttpCookie;
import java.text.ParseException;
import java.util.Optional;

/**
 * Represents all "Set-Cookie" fields in a Response header. Several cookies are
 * actually set with several headers. However, to provide uniform access to all
 * header fields, they are converted to a field with a list of values in the
 * internal representation.
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
	 * @param s
	 *            the string to parse
	 * @return this object for easy chaining
	 * @throws ParseException
	 *             if the input violates the field format
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
	 *             if the input violates the field format
	 */
	public static HttpSetCookieListField fromString(String s)
	        throws ParseException {
		HttpSetCookieListField result = new HttpSetCookieListField();
		result.addFromString(s);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jdrupes.httpcodec.fields.HttpListField#elementToString(java.lang.
	 * Object)
	 */
	@Override
	protected String elementToString(HttpCookie element) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the value for the cookie with the given name.
	 * 
	 * @param name
	 *            the name
	 * @return the value if a cookie with the given name exists
	 */
	public Optional<String> valueForName(String name) {
		return stream().filter(cookie -> cookie.getName().equals(name))
			.findFirst().map(HttpCookie::getValue);
	}
}
