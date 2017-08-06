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

import java.util.Map;

import org.jgrapes.core.Event;

/**
 * A query event for a key/value store.
 */
public class KeyValueStoreQuery extends Event<Map<String,String>> {

	private String key;

	/**
	 * Creates a new event that queries using the given key. The
	 * result of the event is a map with the retrieved entries.
	 * 
	 * @param key the key
	 */
	public KeyValueStoreQuery(String key) {
		this.key = key;
	}

	/**
	 * Convenience constructor for creating a new event with
	 * a completion event of type {@link KeyValueStoreData}.
	 * 
	 * @param key the key
	 * @param completionEvent
	 */
	public KeyValueStoreQuery(String key, boolean completionEvent) {
		this(key);
		if (completionEvent) {
			new KeyValueStoreData(this);
		}
	}

	/**
	 * Returns the key used for the query.
	 * 
	 * @return the key
	 */
	public String query() {
		return key;
	}
	
	
}
