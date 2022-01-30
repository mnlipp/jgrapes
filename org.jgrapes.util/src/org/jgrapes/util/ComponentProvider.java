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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
 * (see {@link #setFactories(ComponentFactory...)}) and component 
 * configurations (see {@link #setPinned(List)} and 
 * {@link #onConfigurationUpdate(ConfigurationUpdate)}). 
 * 
 * For each configuration that references a known factory, a component is 
 * created and attached to this component provider as child.
 * 
 * @since 1.3
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ComponentProvider extends Component {

    /** The entry name for the component's type. */
    public static final String COMPONENT_TYPE = "componentType";
    /** The entry name for the component's name. */
    public static final String COMPONENT_NAME = "name";

    private String componentsEntry = "components";
    private Map<String, ComponentFactory> factoryByType
        = Collections.emptyMap();
    private List<Map<Object, Object>> currentConfig = Collections.emptyList();
    private List<Map<Object, Object>> pinnedConfigurations
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
     * Sets the name of the entry in this component's configuration
     * information (as returned by 
     * {@link #providerConfiguration(ConfigurationUpdate)})
     * that holds the information about the components to be provided.
     * Defaults to "components". 
     * 
     * If set to `null`, handling {@link ConfigurationUpdate} events 
     * is effectively disabled (unless 
     * {@link #componentConfigurations(ConfigurationUpdate)}
     * is overridden by a method that ignores the setting).
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
        factoryByType = Collections.unmodifiableMap(Arrays.stream(factories)
            .collect(Collectors
                .toMap(f -> f.componentType().getName(), Function.identity(),
                    (a, b) -> b)));
        synchronize(currentConfig);
        return this;
    }

    /**
     * Gets the factories as a map, indexed by component type.
     *
     * @return the factories
     */
    public Map<String, ComponentFactory> factories() {
        return factoryByType;
    }

    /**
     * Sets the pinned configurations. Components provided due to
     * these configurations exist independent of any information passed by
     * {@link ConfigurationUpdate} events.
     *
     * @param pinnedConfigurations the configurations to be pinned
     * @return the component provider for easy chaining
     */
    @SuppressWarnings("unchecked")
    public ComponentProvider setPinned(List<Map<?, ?>> pinnedConfigurations) {
        this.pinnedConfigurations
            = Collections.unmodifiableList(pinnedConfigurations.stream()
                .map(c -> Collections
                    .unmodifiableMap(new HashMap<>((Map<Object, Object>) c)))
                .collect(Collectors.toList()));
        synchronize(currentConfig);
        return this;
    }

    /**
     * Gets the pinned configurations.
     *
     * @return the pinned configurations
     */
    public List<Map<Object, Object>> pinned() {
        return pinnedConfigurations;
    }

    /**
     * Selects configuration information targeted at this component
     * from the event. The default implementation invokes 
     * {@link ConfigurationUpdate#structured(String)} with this
     * component's path to obtain the information. Called by 
     * {@link #componentConfigurations(ConfigurationUpdate)}.
     *
     * @param evt the event
     * @return the configuration information as provided by
     * {@link ConfigurationUpdate#structured(String)} if it exists
     */
    protected Optional<Map<String, ?>>
            providerConfiguration(ConfigurationUpdate evt) {
        return evt.structured(componentPath());
    }

    /**
     * Retrieves the configurations for components to be provided
     * from an entry in a {@link ConfigurationUpdate} event.
     * Overriding this method enables derived classes to fully 
     * control how this information is retrieved from the
     * {@link ConfigurationUpdate} event.
     * 
     * This implementation of the method calls 
     * {@link #providerConfiguration(ConfigurationUpdate)} to obtain
     * all configuration information targeted at this component provider.
     * It then uses the configured entry 
     * (see {@link #setComponentsEntry(String)}) to retrieve the information
     * about the components to be provided.
     * 
     * The method must ensure that the result is a collection
     * of maps, where each map has at least entries with
     * keys "componentType" and "name", each associated with a
     * value of type {@link String}.
     * 
     * @param evt the event
     * @return the collection
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    protected List<Map<Object, Object>>
            componentConfigurations(ConfigurationUpdate evt) {
        if (componentsEntry == null) {
            // Shortcut, avoids call to provider configuration.
            return Collections.emptyList();
        }
        return providerConfiguration(evt)
            .map(conf -> conf.get(componentsEntry))
            .filter(Collection.class::isInstance).map(c -> (Collection<?>) c)
            .orElse(Collections.emptyList()).stream()
            .filter(Map.class::isInstance).map(c -> (Map<?, ?>) c)
            .filter(c -> c.keySet()
                .containsAll(Set.of(COMPONENT_TYPE, COMPONENT_NAME))
                && String.class.isInstance(c.get(COMPONENT_TYPE))
                && String.class.isInstance(c.get(COMPONENT_NAME)))
            .map(c -> {
                @SuppressWarnings("unchecked") // Checked for relevant entries
                var casted = (Map<Object, Object>) c;
                return casted;
            })
            .collect(Collectors.toList());
    }

    /**
     * Uses the information from the event to configure the
     * provided components.
     * 
     * @see #componentConfigurations(ConfigurationUpdate)
     * @param evt the event
     */
    @Handler
    public void onConfigurationUpdate(ConfigurationUpdate evt) {
        synchronize(componentConfigurations(evt));
    }

    private void synchronize(List<Map<Object, Object>> requested) {
        synchronized (this) {
            // Calculate starters for to be added/to be removed
            var toBeAdded = new LinkedList<>(requested);
            toBeAdded.addAll(pinnedConfigurations);
            var toBeRemoved = children().stream()
                .map(c -> Components.manager(c))
                .collect(Collectors.toCollection(LinkedList::new));

            // Don't attempt to add something that we have no factory for.
            toBeAdded = toBeAdded.stream()
                .filter(c -> factoryByType.containsKey(c.get(COMPONENT_TYPE)))
                .collect(Collectors.toCollection(LinkedList::new));

            // Remove the intersection of "to be added" and "to be removed"
            // from both, thus leaving what their names say.
            for (var childIter = toBeRemoved.iterator(); childIter.hasNext();) {
                var child = childIter.next();
                @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
                var childComp = child.component().getClass().getName();
                var childName = child.name();
                for (var confIter = toBeAdded.iterator();
                        confIter.hasNext();) {
                    var config = confIter.next();
                    var confComp = config.get(COMPONENT_TYPE);
                    var confName = config.get(COMPONENT_NAME);
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
                return factoryByType.get(config.get(COMPONENT_TYPE))
                    .create(channel(), config).map(
                        c -> ComponentFactory.setStandardProperties(c, config))
                    .stream();
            }).flatMap(Function.identity())
                .forEach(component -> attach(component));

            // Save configuration as current
            currentConfig = requested;
        }
    }

}
