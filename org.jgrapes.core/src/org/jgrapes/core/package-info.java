/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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

/**
 * This package defines the interfaces and classes that provide
 * the core functionality of the JGrapes event driven component framework.
 * 
 * A JGrapes application consists of a tree of components that interact
 * using events.
 * 
 * ![Sample component tree](ComponentTree.svg)
 *  
 * Components can be defined in two ways. Classes can implement the interface
 * {@link org.jgrapes.core.ComponentType} and provide a special attribute
 * that allows them to access their component manager
 * (see the description of the interface {@link org.jgrapes.core.ComponentType}
 * for details). Alternatively, classes
 * can inherit from {@link org.jgrapes.core.Component}. This base class
 * implements {@link org.jgrapes.core.ComponentType} and also provides the 
 * component manager for the component.
 *  
 * ![Example](Components.svg)
 * 
 * The {@link org.jgrapes.core.Manager} interface enables the components 
 * to access the functions of the framework. This includes methods
 * for manipulating the tree structure 
 * ({@link org.jgrapes.core.Manager#attach(ComponentType)},
 * {@link org.jgrapes.core.Manager#detach()} etc.) and methods
 * for sending and handling events
 * ({@link org.jgrapes.core.Manager#fire(Event, Channel...)},
 * {@link org.jgrapes.core.Manager#addHandler(Method, HandlerScope, int)}).
 * 
 * Logging
 * -------
 * 
 * The package supports some specific (java.util) logging settings.
 * <dl>
 *   <dt>{@code org.jgrapes.core.handlerTracking.level}</dt>
 *   <dd>If set to {@code FINE}, causes events and their handlers to be logged
 *   before the handler is invoked. If set to {@code FINER} additionally 
 *   causes events without any handler to be logged and if set to {@code
 *   FINEST} also logs the result of invoking a handler
 *   (the additional logging is done with log level
 *   {@code FINE}, just as the logging of the invocations). Enabling the
 *   invocation logging involves some performance penalty because additional
 *   information (not required for normal operation) has to be maintained.</dd>
 *   
 *   <dt>{@code org.jgrapes.core.classNames.level}</dt>
 *   <dd>If set to {@code FINER}, class names are converted to fully
 *   qualified names in {@code toString()} methods.</dd>
 *   </dl>
 * 
 *
 * @startuml ComponentTree.svg
 * object Component1 #NavajoWhite
 * object Component2 #NavajoWhite
 * object Component3 #NavajoWhite
 * 
 * Component1 -- Component2
 * Component1 -- Component3
 * 
 * @enduml
 * 
 * @startuml Components.svg
 * 
 * interface Manager {
 *   T attach(T child)
 *   ComponentType detach()
 *   List<ComponentType> children()
 *   ComponentType parent()
 *   ComponentType root()
 *   Channel channel()
 *   Event<T> fire(Event<T> event, Channel[] channels)
 *   void addHandler(Method method, HandlerScope scope, int priority)
 * }
 * interface ComponentType
 * class ComponentWithOwnBaseClass #NavajoWhite {
 *   -<<ComponentManager>> manager: Manager 
 * }
 * class SomeComponent #NavajoWhite
 * 
 * ComponentWithOwnBaseClass *-> "1" Manager
 * Manager <|.. Component
 * ComponentType <|.. Component
 * Component <|-- SomeComponent
 * ComponentType <|.. ComponentWithOwnBaseClass
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.core;
