/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionEvent;

/**
 * Defines an optional completion event for a {@link KeyValueStoreQuery}.
 * 
 * Because {@link KeyValueStoreQuery} events have a result, this event
 * is not automatically generated for every {@link KeyValueStoreQuery}.
 * Rather, it must be created explicitly.
 */
public class KeyValueStoreData extends CompletionEvent<KeyValueStoreQuery> {

	/**
	 * @param monitoredEvent
	 * @param channels
	 */
	public KeyValueStoreData(KeyValueStoreQuery monitoredEvent,
	        Channel... channels) {
		super(monitoredEvent, channels);
	}

	/**
	 * A shortcut to get the result of the completed query event.
	 * 
	 * @return the data
	 */
	public Map<String,String> data() {
		try {
			return event().get();
		} catch (InterruptedException e) {
			// Can only happen if invoked before completion
			throw new IllegalStateException(e);
		}
	}
}
