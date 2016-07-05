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
public class HttpListFieldValue extends HttpFieldValue
	implements List<String> {

	private List<String> elements = new ArrayList<>();
	
	/**
	 * Creates the new object from the given value.
	 * 
	 * @param value the value
	 * @throws ParseException 
	 */
	public HttpListFieldValue(String value) throws ParseException {
		super(value);
		while (true) {
			String element = nextElement();
			if (element == null) {
				break;
			}
			elements.add(HttpFieldValue.unquote(element));
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String asString() {
		boolean first = true;
		StringBuilder result = new StringBuilder();
		for (String s: this) {
			if (first) {
				first = false;
			} else {
				result.append(", ");
			}
			result.append(quoteIfNecessary(s));
		}
		return result.toString();
	}

	/**
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, String element) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(String e) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends String> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection<? extends String> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		throw new UnsupportedOperationException();
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
	public String get(int index) {
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
	public Iterator<String> iterator() {
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
	public ListIterator<String> listIterator() {
		return elements.listIterator();
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<String> listIterator(int index) {
		return elements.listIterator(index);
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public String remove(int index) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public String set(int index, String element) {
		throw new UnsupportedOperationException();
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
	public List<String> subList(int fromIndex, int toIndex) {
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
	public <T> T[] toArray(T[] a) {
		return elements.toArray(a);
	}
	
}
