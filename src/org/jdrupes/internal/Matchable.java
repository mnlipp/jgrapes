/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes.internal;

/**
 * @author mnl
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
	 * Returns <code>true</code> if this Matchable matches the
	 * key used as filter by a handler.
	 * 
	 * @param handlerKey the key used by the handler
	 */
	boolean matches(Object handlerKey);
}
