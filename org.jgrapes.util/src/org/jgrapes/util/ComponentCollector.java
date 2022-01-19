/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentFactory;

/**
 * A component that collects all component factory services of 
 * a given type and uses each to create one or more components
 * that are then attached to the component collector instance.
 * 
 * Effectively, the component collector leverages the 
 * mechanism provided by the service loader to determine
 * the component tree that is built eventually at 
 * "link time".
 * 
 * @param <F> the component factory type
 */
public class ComponentCollector<F extends ComponentFactory>
        extends Component {

    private static final List<Map<Object, Object>> SINGLE_DEFAULT
        = Arrays.asList(Collections.emptyMap());

    /**
     * Creates a new collector that collects the factories of the given 
     * type and uses each to create one or more instances with this 
     * component's (the component collector's) channel. 
     * 
     * Before instances are created, the `matcher` function is 
     * invoked with the name of the class of the component
     * to be created as argument. The list of maps returned is
     * used to create components, passing each element in the list
     * as parameter to {@link ComponentFactory#create(Channel, Map)}.
     * 
     * @param factoryClass the factory class
     * @param componentChannel this component's channel
     * @param matcher the matcher function
     */
    public ComponentCollector(
            Class<F> factoryClass, Channel componentChannel,
            Function<String, List<Map<Object, Object>>> matcher) {
        super(componentChannel);
        ServiceLoader<F> serviceLoader = ServiceLoader.load(factoryClass);
        for (F factory : serviceLoader) {
            List<Map<Object, Object>> configs = matcher.apply(
                factory.componentType().getName());
            for (Map<Object, Object> config : configs) {
                factory.create(channel(), config).ifPresent(
                    component -> attach(component));
            }
        }
    }

    /**
     * Utility constructor that uses each factory to create a single
     * instance, using an empty map as properties.
     * 
     * @param factoryClass the factory class
     * @param componentChannel this component's channel
     */
    public ComponentCollector(
            Class<F> factoryClass, Channel componentChannel) {
        this(factoryClass, componentChannel, type -> SINGLE_DEFAULT);
    }
}
