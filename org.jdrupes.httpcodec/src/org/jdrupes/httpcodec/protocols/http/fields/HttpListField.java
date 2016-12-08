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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An HTTP field value that consists of a list of separated by a delimiter
 * strings. The class provides a "list of field values" view of the values.
 * 
 * @author Michael N. Lipp
 */
public abstract class HttpListField<T> extends HttpField<List<T>>
	implements List<T>, Cloneable {

	private String unparsedValue;
	private int position;
	private List<T> elements = new ArrayList<>();
	
	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this
	 * initial state, the field is invalid and no string representation
	 * can be generated. This constructor must be followed by method invocations
	 * that add values.
	 * 
	 * @param name the field name
	 */
	protected HttpListField(String name) {
		super(name);
		reset();
	}

	/**
	 * Creates a new header field object with the given field name and unparsed
	 * value.
	 * 
	 * @param name
	 *            the field name
	 * @param unparsedValue
	 * 			  the field value as it appears in the HTTP header
	 */
	protected HttpListField(String name, String unparsedValue) {
		this(name);
		this.unparsedValue = unparsedValue;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public HttpListField<T> clone() {
		HttpListField<T> result = (HttpListField<T>)super.clone();
		result.elements = new ArrayList<>(elements);
		return result;
	}

	/**
	 * Returns the char that separates the items in the list.
	 * 
	 * @return the default implementation returns a comma
	 */
	protected char getDelimiter() {
		// Used by default in RFC 7230, see section 7.
		return ',';
	}
	
	/**
	 * Reset the parsing state.
	 */
	protected void reset() {
		position = 0;
	}
	
	/**
	 * Returns the next element from the unparsed value.
	 * 
	 * @return the next element or {@code null} if no elements remain
	 * @throws ParseException if the input violates the field format
	 */
	protected String nextElement() throws ParseException {
		// RFC 7230 3.2.6
		boolean inDquote = false;
		int startPosition = position;
		char separator = getDelimiter();
		try {
			while (true) {
				if (inDquote) {
					char ch = unparsedValue.charAt(position);
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
				if (position == unparsedValue.length()) {
					if (position == startPosition) {
						return null;
					}
					return unparsedValue.substring(startPosition, position);
				}
				char ch = unparsedValue.charAt(position);
				if (ch == separator) {
					String result = unparsedValue
					        .substring(startPosition, position);
					position += 1; // Skip comma
					while (true) { // Skip optional white space
						ch = unparsedValue.charAt(position);
						if (ch != ' ' && ch != '\t') {
							break;
						}
						position += 1;
					}
					return result;
				}
				switch (ch) {
				case '\"':
					inDquote = true;
				default:
					position += 1;
					continue;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(unparsedValue, position);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpField#getValue()
	 */
	@Override
	public List<T> getValue() {
		return this;
	}

	protected abstract String elementToString(T element);

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asFieldValue() {
		if (size() == 0) {
			throw new IllegalStateException(
			        "Field with list value may not be empty.");
		}
		char separator = getDelimiter();
		boolean first = true;
		StringBuilder result = new StringBuilder();
		for (T e: this) {
			if (first) {
				first = false;
			} else {
				result.append(separator);
				result.append(" ");
			}
			result.append(elementToString(e));
		}
		return result.toString();
	}

	/**
	 * Appends the value to the list of values.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public HttpListField<T> append(T value) {
		elements.add(value);
		return this;
	}
	
	/**
	 * Appends the value to the list of values if it is not already in the list.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public HttpListField<T> appendIfNotContained(T value) {
		if (!elements.contains(value)) {
			elements.add(value);
		}
		return this;
	}
	
	/**
	 * Combine this list with another list of the same type.
	 * 
	 * @param other the other list
	 */
	@SuppressWarnings("unchecked")
	public void combine(@SuppressWarnings("rawtypes") HttpListField other) {
		if (!(getClass().equals(other.getClass()))
				|| !getName().equals(other.getName())) {
			throw new IllegalArgumentException("Types and name must be equal.");
		}
		addAll(other);
	}

	/**
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, T element) {
		elements.add(index, element);
	}

	/**
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(T e) {
		return elements.add(e);
	}

	/**
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends T> c) {
		return elements.addAll(c);
	}

	/**
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection<? extends T> c) {
		return elements.addAll(index, c);
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		elements.clear();
	}

	/**
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return elements.contains(o);
	}

	/**
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return elements.containsAll(c);
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public T get(int index) {
		return elements.get(index);
	}

	/**
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object o) {
		return elements.indexOf(o);
	}

	/**
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	/**
	 * @see java.util.List#iterator()
	 */
	public Iterator<T> iterator() {
		return elements.iterator();
	}

	/**
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object o) {
		return elements.lastIndexOf(o);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator<T> listIterator() {
		return elements.listIterator();
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<T> listIterator(int index) {
		return elements.listIterator(index);
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public T remove(int index) {
		return elements.remove(index);
	}

	/**
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		return elements.remove(o);
	}

	/**
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		return elements.removeAll(c);
	}

	/**
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		return elements.retainAll(c);
	}

	/**
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public T set(int index, T element) {
		return elements.set(index, element);
	}

	/**
	 * @see java.util.List#size()
	 */
	public int size() {
		return elements.size();
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public List<T> subList(int fromIndex, int toIndex) {
		return elements.subList(fromIndex, toIndex);
	}

	/**
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return elements.toArray();
	}

	/**
	 * @see java.util.List#toArray(java.lang.Object[])
	 */
	public <U> U[] toArray(U[] a) {
		return elements.toArray(a);
	}
	
}
