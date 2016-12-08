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
import java.util.Arrays;

/**
 * An HTTP field value that consists of a comma separated list of 
 * integers. The class provides a "list of integers" view
 * of the values.
 * 
 * @author Michael N. Lipp
 */
public class HttpIntListField extends HttpListField<Long>
	implements Cloneable {

	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this
	 * initial state, the field is invalid and no string representation
	 * can be generated. This constructor must be followed by method invocations
	 * that add values.
	 * 
	 * @param name the field name
	 */
	public HttpIntListField(String name) {
		super(name);
		reset();
	}

	/**
	 * Creates the new object from the given values.
	 * 
	 * @param name the field name
	 * @param value the first value
	 * @param values more values
	 */
	public HttpIntListField(String name, Long value, Long... values) {
		super(name);
		add(value);
		addAll(Arrays.asList(values));
	}

	/**
	 * Creates a new object with the given field name and unparsed value.
	 * 
	 * @param name the field name
	 * @param unparsedValue the unparsed value
	 * @throws ParseException if the input violates the field format
	 */
	public HttpIntListField(String name, String unparsedValue)
			throws ParseException {
		super(name, unparsedValue);
		while (true) {
			String element = nextElement();
			if (element == null) {
				break;
			}
			try {
				add(Long.parseLong(element));
			} catch (NumberFormatException e) {
				throw new ParseException(element, 0);
			}
		}
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param name the field name
	 * @param s the string to parse
	 * @return the result
	 * @throws ParseException if the input violates the field format
	 */
	public static HttpIntListField fromString(String name, String s) 
			throws ParseException {
		return new HttpIntListField(name, s);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public HttpIntListField clone() {
		return (HttpIntListField)super.clone();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpListField#elementAsString(java.lang.Object)
	 */
	@Override
	protected String elementToString(Long element) {
		return element.toString();
	}

}
