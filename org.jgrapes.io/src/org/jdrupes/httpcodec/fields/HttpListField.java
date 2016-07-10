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

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An HTTP field value that consists of a comma separated list of 
 * strings. The class provides an unmodifiable list of strings view
 * of the values.
 * 
 * @author Michael N. Lipp
 */
public abstract class HttpListField<T> extends HttpField<List<T>>
	implements List<T> {

	private String unparsedValue;
	private int position;
	private List<T> elements = new ArrayList<>();
	
	/**
	 * Creates a new object with the given field name and no elements.
	 * 
	 * @param name the field name
	 */
	protected HttpListField(String name) {
		super(name);
		reset();
	}

	/**
	 * Creates a new object with the given field name and unparsed value.
	 * 
	 * @param name the field name
	 */
	protected HttpListField(String name, String unparsedValue) {
		this(name);
		this.unparsedValue = unparsedValue;
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
	 * @throws ParseException 
	 */
	protected String nextElement() throws ParseException {
		boolean inDquote = false;
		int startPosition = position;
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
				switch (ch) {
				case ',':
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
	public String valueToString() {
		boolean first = true;
		StringBuilder result = new StringBuilder();
		for (T e: this) {
			if (first) {
				first = false;
			} else {
				result.append(", ");
			}
			result.append(elementToString(e));
		}
		return result.toString();
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
