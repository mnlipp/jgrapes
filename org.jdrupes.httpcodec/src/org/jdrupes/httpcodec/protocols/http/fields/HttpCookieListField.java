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
	 * @return the result
	 * @throws ParseException if the input violates the field format
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
	 * @return the value if a cookie with the given name exists
	 */
	public Optional<String> valueForName(String name) {
		return stream().filter(cookie -> cookie.getName().equals(name))
				.findFirst().map(HttpCookie::getValue);
	}
}
