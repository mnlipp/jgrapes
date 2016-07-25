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
import java.util.Arrays;

/**
 * An HTTP field value that consists of a comma separated list of 
 * strings. The class provides a "list of strings" view
 * of the values.
 * 
 * @author Michael N. Lipp
 */
public class HttpStringListField extends HttpListField<String>
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
	public HttpStringListField(String name) {
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
	public HttpStringListField(String name, String value, String... values) {
		super(name);
		add(value);
		addAll(Arrays.asList(values));
	}

	/**
	 * Creates a new object with the given field name and unparsed value.
	 * 
	 * @param name the field name
	 * @param unparsedValue the unparsed value
	 * @param unparsed used to distinguish constructors
	 */
	protected HttpStringListField(String name, String unparsedValue, 
			boolean unparsed) {
		super(name, unparsedValue);
	}

	/**
	 * Creates a new object with the elements obtained by parsing the given
	 * String.
	 * 
	 * @param name the field name
	 * @param s the string to parse
	 * @throws ParseException 
	 */
	public static HttpStringListField fromString(String name, String s) 
			throws ParseException {
		HttpStringListField result = new HttpStringListField(name, s, true);
		while (true) {
			String element = result.nextElement();
			if (element == null) {
				break;
			}
			result.add(unquote(element));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public HttpStringListField clone() {
		return (HttpStringListField)super.clone();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpListField#elementAsString(java.lang.Object)
	 */
	@Override
	protected String elementToString(String element) {
		return quoteIfNecessary(element);
	}

	public static HttpStringListField cast(HttpListField<String> field) {
		return (HttpStringListField)field;
	}
	
	/**
	 * Returns whether the list contains the given value, ignoring
	 * differences in the cases of the letters.
	 * 
	 * @param value the value to compare with
	 * @return the result
	 */
	public boolean containsIgnoreCase(String value) {
		for (String s: getValue()) {
			if (s.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes all strings equal to the given value, ignoring
	 * differences in the cases of the letters.
	 * 
	 * @param value the value to compare with
	 */
	public void removeIgnoreCase(String value) {
		removeIf(s -> s.equalsIgnoreCase(value));
	}
}
