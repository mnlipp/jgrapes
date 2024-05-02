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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * A component that collects all component factory services of 
 * a given type and uses each to create one or more components
 * that are then attached to the component collector instance.
 * 
 * Effectively, the component collector leverages the 
 * mechanism provided by the service loader to configure
 * the component subtree rooted at the collector at "link time".
 * 
 * This class uses {@link ComponentProvider#setFactories(ComponentFactory...)} 
 * and {@link ComponentProvider#setPinned(List)} for its implementation.
 * As it inherits from {@link ComponentProvider}, it automatically
 * supports the provisioning of additional components through
 * {@link ConfigurationUpdate} events. If this is not desired, invoke
 * {@link ComponentProvider#setComponentsEntry(String)} with `null` as
 * argument. 
 * 
 * @param <F> the component factory type
 */
public class ComponentCollector<F extends ComponentFactory>
        extends ComponentProvider {

    private static final List<Map<Object, Object>> SINGLE_DEFAULT
        = List.of(Collections.emptyMap());

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
     * @param configurator the configurator function
     */
    public ComponentCollector(Class<F> factoryClass, Channel componentChannel,
            Function<String, List<Map<Object, Object>>> configurator) {
        super(componentChannel);
        ServiceLoader<F> serviceLoader = ServiceLoader.load(factoryClass);

        // Get all factories
        IntFunction<F[]> createFactoryArray = size -> {
            @SuppressWarnings("unchecked")
            var res = (F[]) Array.newInstance(factoryClass, size);
            return res;
        };
        var factories = serviceLoader.stream().map(Provider::get)
            .toArray(createFactoryArray);
        setFactories(factories);

        // Obtain a configuration for each factory
        List<Map<?, ?>> configs = Arrays.stream(factories)
            .map(factory -> configurator
                .apply(factory.componentType().getName()).stream().map(c -> {
                    if (c.containsKey(COMPONENT_TYPE)) {
                        return c;
                    }
                    // The map may be immutable, copy.
                    @SuppressWarnings("PMD.UseConcurrentHashMap")
                    Map<Object, Object> newMap = new HashMap<>(c);
                    newMap.put(COMPONENT_TYPE,
                        factory.componentType().getName());
                    return newMap;
                }))
            .flatMap(Function.identity()).collect(Collectors.toList());
        setPinned(configs);
    }

    /**
     * Utility constructor that uses each factory to create a single
     * instance, using an empty map as properties.
     * 
     * @param factoryClass the factory class
     * @param componentChannel this component's channel
     */
    public ComponentCollector(Class<F> factoryClass, Channel componentChannel) {
        this(factoryClass, componentChannel, type -> SINGLE_DEFAULT);
    }
}
