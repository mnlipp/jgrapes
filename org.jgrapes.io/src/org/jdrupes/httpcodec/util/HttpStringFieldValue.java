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
package org.jdrupes.httpcodec.util;

/**
 * Represents a header field's value and provides methods for interpreting
 * that value.
 * 
 * @author Michael N. Lipp
 */
public class HttpStringFieldValue extends HttpFieldValue {

	/**
	 * Creates a new representation of a field value.
	 * 
	 * @param value
	 */
	public HttpStringFieldValue(String value) {
		super(value);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asString() {
		return quoteIfNecessary(rawValue());
	}
}
