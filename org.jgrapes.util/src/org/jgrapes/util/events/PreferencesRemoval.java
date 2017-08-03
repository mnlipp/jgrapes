/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.util.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jgrapes.core.Event;

/**
 * Indicates the removal of all preference starting with the given paths.
 */
public class PreferencesRemoval extends Event<Void> {

	private Set<String> paths = new HashSet<>();

	/**
	 * Adds a new path to the removed paths.
	 * 
	 * @param path the path
	 * @return the event for easy chaining
	 */
	public PreferencesRemoval add(String path) {
		if (path == null) {
			path = "";
		}
		paths.add(path);
		return this;
	}

	/**
	 * Returns all removed paths.
	 * 
	 * @return the paths
	 */
	public Set<String> paths() {
		return Collections.unmodifiableSet(paths);
	}
}
