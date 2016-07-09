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
 * An HTTP field value that is an integer.
 * 
 * @author Michael N. Lipp
 */
public class HttpIntField extends HttpField<Long> {

	private long parsedValue;
	
	/**
	 * Creates the object and parses the value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 * @throws ParseException 
	 */
	public HttpIntField(String name, String value) throws ParseException {
		super(name, value);
		try {
			parsedValue = Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new ParseException(value, 0);
		}
	}

	/**
	 * Creates the object with the given value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 * @throws ParseException 
	 */
	public HttpIntField(String name, long value) {
		super(name, Long.toString(value));
		parsedValue = value;
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	@Override
	public Long getValue() {
		return parsedValue;
	}
	
	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public int asInt() {
		return (int)parsedValue;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String valueToString() {
		return Long.toString(parsedValue);
	}
	
	
}
