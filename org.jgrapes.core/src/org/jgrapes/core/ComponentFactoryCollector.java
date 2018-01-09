/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017 Michael N. Lipp
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

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.ComponentType;

/**
 * A component that collects all services of 
 * a given type and creates an instance of each.
 * 
 * @param <F> the component factory type
 */
public class ComponentFactoryCollector<F extends ComponentFactory>
	extends Component {

	/**
	 * Creates a new collector that collects the factories of the given 
	 * type and uses each to create an instance with this component's
	 * channel. Before the instance is created, the `matcher` 
	 * function is invoked with the factory's component's class 
	 * as argument. If the function returns `null`,
	 * the creation is skipped. Else the map returned is
	 * passed as parameter to 
	 * {@link ComponentFactory#create(Channel, Map)}
	 * 
	 * @param cls the factory class
	 * @param componentChannel this component's channel
	 * @param matcher the matcher function
	 */
	public ComponentFactoryCollector(
			Class<F> factoryClass, Channel componentChannel,
			Function<Class<? extends ComponentType>,Map<Object,Object>> matcher) {
		super(componentChannel);
		ServiceLoader<F> serviceLoader = ServiceLoader.load(factoryClass);
		for (Iterator<F> itr = serviceLoader.iterator(); itr.hasNext();) {
			F factory = itr.next();
			Map<Object,Object> config = matcher.apply(factory.componentType());
			if (config != null) {
				factory.create(channel(), config).ifPresent(
						component -> attach(component));
			}
		}
	}

}
