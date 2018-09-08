/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.core;

import org.jgrapes.core.annotation.ComponentManager;

/**
 * This interface marks a class as a component. Implementing this interface is 
 * an alternative to deriving from {@link Component} (usually because 
 * there is some other preferential inheritance relationship). 
 * Components that implement this interface (but don't inherit from
 * {@link Component}) aren't inserted as vertices into the component tree;
 * rather, they are represented in the tree by a proxy. 
 * 
 * Classes that implement {@code ComponentType} aren't required to
 * implement specific methods. They must, however, declare a field
 * for a component manager. This field must be of type 
 * {@link Manager} and annotated as {@link ComponentManager}.
 * The implementation of the component type can use the value in this 
 * field to get access to the component hierarchy. The field is initialized
 * when the component type is added to the component hierarchy or when
 * calling {@link Components#manager(ComponentType)} or
 * {@link Components#manager(ComponentType, Channel)}.
 * 
 * The channel associated with an instance of {@link ComponentType}
 * can be specified as attribute of the {@link ComponentManager}
 * annotation. If no channel is specified, the proxy is used as
 * channel (see {@link Channel#SELF}). The default can be overridden
 * on a per instance basis by calling 
 * {@link Components#manager(ComponentType, Channel)} before the component
 * is added to the tree.
 */
public interface ComponentType {

}
