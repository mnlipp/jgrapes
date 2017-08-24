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

package org.jgrapes.core;

import java.util.Collections;
import java.util.Map;

/**
 * A general purpose factory interface for components.
 */
public interface ComponentFactory<T extends ComponentType> {

	/**
	 * Returns the type of the components created by this factory.
	 * 
	 * @return the component type
	 */
	Class<T> componentType();
	
	/**
	 * Creates a new component with its channel set to
	 * itself.
	 * 
	 * @return the component
	 */
	default T create() {
		return create(Channel.SELF);
	}
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to
	 * @return the component
	 */
	default T create(Channel componentChannel) {
		return create(componentChannel, Collections.emptyMap());
	}
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel using the given additional properties.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 * @param properties additional properties for the creation of the
	 * component
	 * @return the component
	 */
	T create(Channel componentChannel, Map<Object, Object> properties);
}
