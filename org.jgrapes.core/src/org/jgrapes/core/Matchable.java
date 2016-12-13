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
 * @author Michael N. Lipp
 *
 */
public interface Matchable {

	/**
	 * Returns the key used for matching. 
	 * 
	 * @return the key which usually is a String, a Class or an instance
	 * of Component (for channels only)
	 */
	Object getMatchKey();
	
	/**
	 * Returns <code>true</code> if this Matchable's key matches the
	 * criterion provided by a handler.
	 * 
	 * @param criterion the criterion provided by the handler
	 * @return {@code true} if the criterion matches
	 */
	boolean matches(Object criterion);
}
