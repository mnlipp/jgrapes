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

package org.jgrapes.core;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * A general purpose factory interface for components.
 * 
 * In some use cases, components may not be known at compile-time, but
 * only made available at run-time. The usual pattern for this is to lookup
 * a factory service using the {@link ServiceLoader} and create the
 * components using the factory (or factories) found.
 * 
 * Because JGrapes components interact with their environment only
 * through events, they do not provide special APIs and it is possible
 * to define this generic factory service interface.
 * Of course, lookup by the {@link ServiceLoader} will usually be done
 * using a derived interface (or base class) that represents the special
 * kind of components required and allows an invocation
 * of {@link ServiceLoader#load(Class)} that returns a "filtered"
 * set of factories.
 */
public interface ComponentFactory {

	/**
	 * Returns the type of the components created by this factory.
	 * 
	 * @return the component type
	 */
	Class<? extends ComponentType> componentType();
	
	/**
	 * Creates a new component with its channel set to
	 * itself.
	 * 
	 * @return the component
	 */
	default ComponentType create() {
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
	default ComponentType create(Channel componentChannel) {
		return create(componentChannel, Collections.emptyMap()).get();
	}
	
	/**
	 * Creates a new component with its channel set to the given 
	 * channel using the given additional properties. If the requested
	 * properties or combination of properties cannot be provided by
	 * the component, the factory may return an empty {@link Optional}. 
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 * @param properties additional properties for the creation of the
	 * component
	 * @return the component
	 */
	Optional<ComponentType> create(
			Channel componentChannel, Map<Object, Object> properties);
}
