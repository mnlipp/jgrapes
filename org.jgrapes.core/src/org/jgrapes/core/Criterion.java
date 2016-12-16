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
package org.jgrapes.core;

/**
 * This interface provides a mechanism for matching an object against a value
 * that identifies the object. Objects that have a unique name can, for example,
 * provide their name as match value. Given this value, it is then possible to
 * search through a collection of objects and identify the object as the one
 * that returns {@code true} for {@link #isMatchedBy(Object)}.
 * <P>
 * In addition to its own match value, a {@code Criterion} can also be matched
 * by some other value, e.g. a value that represents a wildcard (objects with
 * unique names could consider their names being matched by their name and "*").
 * The interpretation of {@link #isMatchedBy(Object)} is completely up to the
 * class that implements it. It should, however, return {@code true} when called
 * with the value obtained from {@link #getMatchValue()}.
 * 
 * @author Michael N. Lipp
 */
public interface Criterion {
	
	/**
	 * Returns <code>true</code> if this {@code Criterion}
	 * is met by the provided value.
	 * 
	 * @param value the value
	 * @return {@code true} if the value matches
	 */
	boolean isMatchedBy(Object value);

	/**
	 * Returns a sample value that matches this criterion and is unique
	 * for the class or object that provides it.
	 * 
	 * @return the value
	 */
	Object getMatchValue();
}
