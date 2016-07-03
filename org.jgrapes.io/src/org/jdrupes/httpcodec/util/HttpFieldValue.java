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

import java.text.ParseException;

/**
 * A base class for all kinds of field values.
 * 
 * @author Michael N. Lipp
 */
public abstract class HttpFieldValue {

	private String value;
	private int position;
	
	/**
	 * Creates a new representation of a field value.
	 * 
	 * @param value
	 */
	public HttpFieldValue(String value) {
		this.value = value.trim();
		reset();
	}

	/**
	 * Returns the unparsed value passed to the constructor.
	 * 
	 * @return the value
	 */
	public String rawValue() {
		return value;
	}
	
	/**
	 * Reset the parsing state.
	 */
	public void reset() {
		position = 0;
	}
	
	/**
	 * Returns the next element from a field value that is formated as a
	 * comma separated list of elements.
	 * 
	 * @return the next element or {@code null} if no elements remain
	 * @throws ParseException 
	 */
	public String nextElement() throws ParseException {
		boolean inDquote = false;
		int startPosition = position;
		try {
			while (true) {
				if (inDquote) {
					char ch = value.charAt(position);
					switch (ch) {
					 case '\\':
						 position += 2;
						 continue;
					 case '\"':
						 inDquote = false;
					 default:
						 position += 1;
						 continue;
					}
				}
				if (position == value.length()) {
					if (position == startPosition) {
						return null;
					}
					return value.substring(startPosition, position);
				}
				char ch = value.charAt(position);
				switch (ch) {
				case ',':
					String result = value.substring(startPosition, position);
					position += 1; // Skip comma
					while (true) { // Skip optional white space
						ch = value.charAt(position);
						if (ch != ' ' && ch != '\t') {
							break;
						}
						position += 1;
					}
					return result;
				case '\"':
					inDquote = true;
				default:
					position += 1;
					continue;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(value, position);
		}
	}
	
	/**
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @param value the value to unquote
	 * @return the unquoted value
	 * @throws ParseException 
	 */
	public static String unquote(String value) throws ParseException {
		if (value.length() == 0 || value.charAt(0) != '\"') {
			return value;
		}
		int startPosition = 1;
		int position = 1;
		try {
			StringBuilder result = new StringBuilder();
			while (true) {
				char ch = value.charAt(position);
				switch (ch) {
				case '\\':
					result.append(value.substring(startPosition, position));
					position += 1;
					result.append(value.charAt(position));
					position += 1;
					startPosition = position;
					continue;
				case '\"':
					if (position != value.length() - 1) {
						throw new ParseException(value, position);
					}
					result.append(value.substring(startPosition, position));
					return result.toString();
				default:
					position += 1;
					continue;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(value, position);
		}
	}

	/**
	 * If the value is double quoted, remove the quotes and escape
	 * characters.
	 * 
	 * @return the unquoted value
	 * @throws ParseException 
	 */
	public String unquote() throws ParseException {
		return unquote (value);
	}

	/**
	 * Returns the given string as double quoted string if necessary.
	 * 
	 * @param value the value to quote if necessary
	 * @return the result
	 */
	public static String quoteIfNecessary(String value) {
		StringBuilder result = new StringBuilder();
		int position = 0;
		boolean needsQuoting = false;
		result.append('"');
		while (position < value.length()) {
			char ch = value.charAt(position++);
			if (!needsQuoting && HttpConsts.TCHARS.indexOf(ch) < 0) {
				needsQuoting = true;
			}
			switch(ch) {
			case '"':
			case '\\':
				result.append('\\');
			default:
				result.append(ch);
			}
		}
		result.append('\"');
		if (needsQuoting) {
			return result.toString();
		}
		return value;
	}
	
	/**
	 * Returns the string representation of this fiel value.
	 * 
	 * @return
	 */
	public abstract String asString();
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HttpFieldValue [");
		if (value != null) {
			builder.append("value=");
			builder.append(asString());
		}
		builder.append("]");
		return builder.toString();
	}
	
	
}
