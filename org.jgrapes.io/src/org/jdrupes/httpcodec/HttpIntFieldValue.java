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
package org.jdrupes.httpcodec;

import java.text.ParseException;

/**
 * An HTTP field value that is an integer.
 * 
 * @author Michael N. Lipp
 */
public class HttpIntFieldValue extends HttpFieldValue {

	private long parsedValue;
	
	/**
	 * Creates the object and parses the value.
	 * 
	 * @param value the field's value (as string)
	 * @throws ParseException 
	 */
	public HttpIntFieldValue(String value) throws ParseException {
		super(value);
		try {
			parsedValue = Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new ParseException(value, 0);
		}
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public int asInteger() {
		return (int)parsedValue;
	}
	
	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public long asLong() {
		return parsedValue;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asString() {
		return Long.toString(parsedValue);
	}
	
	
}
