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
import java.util.Iterator;

/**
 * An HTTP field value that consists of a comma separated list of 
 * strings. The class provides an unmodifiable list of strings view
 * of the values.
 * 
 * @author Michael N. Lipp
 */
public class HttpStringListField extends HttpListField<String> {

	/**
	 * Creates the new object from the given value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 * @throws ParseException 
	 */
	public HttpStringListField(String name, String value) throws ParseException {
		super(name, value);
		while (true) {
			String element = nextElement();
			if (element == null) {
				break;
			}
			add(HttpField.unquote(element));
		}
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
	 * @return the result
	 */
	public void removeAllIgnoreCase(String value) {
		for (Iterator<String> iter = getValue().iterator(); iter.hasNext();) {
			if (iter.next().equalsIgnoreCase(value)) {
				iter.remove();
			}
		}
	}
}
