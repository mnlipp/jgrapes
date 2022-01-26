/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentFactory;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.events.ConfigurationUpdate;

/**
 * Provides child components dynamically using {@link ComponentFactory}s.
 * 
 * An instance is configured with a collection of {@link ComponentFactory}s
 * and component configurations. For each configuration where the
 * referenced factory exists, a component is created and attached to
 * this component provider as child.
 * 
 * The component configurations can be updated by
 * {@link ConfigurationUpdate} events.
 * 
 * @since 1.3
 */
public class ComponentProvider extends Component {

    private String componentsEntry = "components";
    private Map<String, ComponentFactory> factoryByType;
    private Collection<Map<String, String>> currentConfig
        = Collections.emptyList();
    private Collection<Map<String, String>> pinnedConfigurations
        = Collections.emptyList();

    /**
     * Creates a new component with its channel set to this object. 
     */
    public ComponentProvider() {
        this(Channel.SELF);
    }

    /**
     * Creates a new component with its channel set to the given 
     * channel. 
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     */
    public ComponentProvider(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets the name of the entry in a {@link ConfigurationUpdate} event,
     * that hold s the information about the components to be provided.
     * Defaults to "components".
     *
     * @param name the name of the entry
     * @return the component provider for easy chaining
     */
    public ComponentProvider setComponentsEntry(String name) {
        this.componentsEntry = name;
        return this;
    }

    /**
     * Sets the factories that this provider knows about. Only
     * configurations with a component type that matches one
     * of the factories are handled by this provider.
     *
     * @param factories the factories
     * @return the component provider for easy chaining
     */
    public ComponentProvider setFactories(ComponentFactory... factories) {
        factoryByType = Arrays.stream(factories).collect(Collectors
            .toMap(f -> f.componentType().getName(), Function.identity(),
                (a, b) -> b));
        synchronize(currentConfig);
        return this;
    }

    /**
     * Sets the pinned configurations. These configurations are
     * in effect independent of any information passed by
     * {@link ConfigurationUpdate} events.
     *
     * @param pinnedConfigurations the configurations to be pinned
     * @return the component provider for easy chaining
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ComponentProvider
            setPinned(Collection<Map<String, ?>> pinnedConfigurations) {
        this.pinnedConfigurations = (Collection<
                Map<String, String>>) (Collection) pinnedConfigurations;
        synchronize(currentConfig);
        return this;
    }

    /**
     * Selects configuration information from the event. The default
     * implementation uses the component's path for obtaining the
     * configuration information. called by 
     * {@link #componentConfigurations(ConfigurationUpdate)}.
     *
     * @param evt the event
     * @return the configuration information as provided by
     * {@link ConfigurationUpdate#structured(String)} if it exists
     * 
     */
    protected Optional<Map<String, ?>>
            providerConfiguration(ConfigurationUpdate evt) {
        return evt.structured(componentPath());
    }

    /**
     * Returns the configurations for the components to be provided.
     * Overriding this method enables derived classes to fully 
     * control how this information is retrieved from the
     * {@link ConfigurationUpdate} event.
     * 
     * This implementation of the method calls 
     * {@link #componentConfigurations(ConfigurationUpdate)} to obtain
     * all configuration information targeted at this component.
     * It then uses the configured entry 
     * (see {@link #setComponentsEntry(String)}) to retrieve the information
     * about the components to be provided.
     * 
     * The method must ensure that the result is a collection
     * of maps, where each map has at least two entries with
     * keys "componentType" and "name", each with an associated
     * value of type {@link String}.
     * 
     * @param evt the event
     * @return the collection
     */
    protected Collection<Map<String, String>>
            componentConfigurations(ConfigurationUpdate evt) {
        return providerConfiguration(evt)
            .map(conf -> conf.get(componentsEntry))
            .filter(Collection.class::isInstance).map(c -> ((Collection<?>) c))
            .orElse(Collections.emptyList()).stream()
            .filter(Map.class::isInstance).map(c -> (Map<?, ?>) c)
            .filter(c -> c.keySet().containsAll(Set.of("componentType", "name"))
                && String.class.isInstance(c.get("componentType"))
                && String.class.isInstance(c.get("name")))
            .map(c -> {
                @SuppressWarnings("unchecked") // Checked for relevant entries
                var casted = (Map<String, String>) c;
                return casted;
            })
            .collect(Collectors.toList());
    }

    /**
     * Uses the information from the event to configure the
     * provided components.
     * 
     * @param evt the event
     */
    @Handler
    public void onConfigurationUpdate(ConfigurationUpdate evt) {
        synchronize(componentConfigurations(evt));
    }

    private synchronized void
            synchronize(Collection<Map<String, String>> requested) {
        // Calculate starters for to be added/to be removed
        var toBeAdded = new LinkedList<>(requested);
        toBeAdded.addAll(pinnedConfigurations);
        var toBeRemoved = children().stream()
            .map(c -> Components.manager(c))
            .collect(Collectors.toCollection(LinkedList::new));

        // Don't attempt to add something that we have no factory for.
        toBeAdded = toBeAdded.stream()
            .filter(c -> factoryByType.containsKey(c.get("componentType")))
            .collect(Collectors.toCollection(LinkedList::new));

        // Remove the intersection of "to be added" and "to be removed" from
        // both, thus leaving what their names say.
        for (var childIter = toBeRemoved.iterator();
                childIter.hasNext();) {
            var child = childIter.next();
            var childComp = child.component().getClass().getName();
            var childName = child.name();
            for (var<Map> confIter = toBeAdded.iterator();
                    confIter.hasNext();) {
                var config = confIter.next();
                var confComp = config.get("componentType");
                var confName = config.get("name");
                if (confComp.equals(childComp)
                    && Objects.equals(childName, confName)) {
                    confIter.remove();
                    childIter.remove();
                }
            }
        }

        // Update children
        for (var child : toBeRemoved) {
            child.detach();
        }
        toBeAdded.stream().map(config -> {
            return factoryByType.get(config.get("componentType"))
                .create(channel(), config).map(
                    c -> ComponentFactory.setStandardProperties(c, config))
                .stream();
        }).flatMap(Function.identity())
            .forEach(component -> attach(component));

        // Save configuration as current
        currentConfig = requested;
    }

}
