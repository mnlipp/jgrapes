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
 * Defines the interfaces and classes that provide
 * the core functionality of the JGrapes event driven component framework.
 * 
 * Components
 * ----------
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
 * Events
 * ------
 * 
 * Events are objects that trigger activities of the components that
 * handle them. Because components are usually only interested in certain 
 * kinds of triggers, events implement the {@link org.jgrapes.core.Eligible}
 * interface that enables the user to obtain an event's kind (as criterion)
 * and filter events depending on their kind.
 * 
 * ![Events](Events.svg)
 * 
 * As implemented in the base class {@link org.jgrapes.core.Event}, the 
 * kind of an event is represented by its Java class. E.g. a 
 * {@link org.jgrapes.core.events.Started} event is an instance 
 * of class `org.jgrapes.core.events.Started` and its kind
 * (obtainable from {@link org.jgrapes.core.Event#defaultCriterion()})
 * is `org.jgrapes.core.events.Started.class`.
 * 
 * Especially when building small sample applications, some programmers 
 * prefer to use a name for representing the kind of an event. The core 
 * package supports this by providing the {@link org.jgrapes.core.NamedEvent}.
 * This class overrides {@link org.jgrapes.core.Event#defaultCriterion()}
 * and {@link org.jgrapes.core.Event#isEligibleFor(Object)} so that a
 * simple string is used to represent and match the event's kind.
 *  
 * Event Handlers
 * --------------
 * 
 * Event handlers are methods that are invoked by the framework.
 * These method have return type `void` and can have zero to
 * two parameters. If specified, the first parameter must be of type
 * {@link org.jgrapes.core.Event} (or, obviously, a type derived from
 * `Event`). The purpose of the second (optional) parameter will be 
 * explained in the next section.
 * 
 * Event handlers are usually registered with the framework using
 * an annotation. The standard annotation for registering event handlers
 * is {@link org.jgrapes.core.annotation.Handler}. See its definition
 * for usage examples. If special needs arise,
 * other annotations for registering handlers may be defined using the 
 * {@link org.jgrapes.core.annotation.HandlerDefinition}.
 * 
 * If the information required for using the handler annotation is not 
 * (completely) available at compile time, handler methods can also be added
 * at runtime using
 * {@link org.jgrapes.core.Manager#addHandler(Method, HandlerScope, int)}.
 * 
 * Channels
 * --------
 * 
 * ...
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
 * 
 * @startuml Events.svg
 * 
 * interface Eligible {
 * 	Object defaultCriterion()
 * 	boolean isEligibleFor(Object criterion)
 * }
 * 
 * class Event<T> {
 * 	+Object defaultCriterion()
 * 	+boolean isEligibleFor(Object criterion)
 * }
 * 
 * Event .right.|> Eligible
 * 
 * class NamedEvent {
 * 	-String name
 * 	+NamedEvent(String name)
 * 	+Object defaultCriterion()
 * 	+boolean isEligibleFor(Object criterion)
 * }
 * Event <|-- NamedEvent
 * 
 * class UserEventType #NavajoWhite {
 * 	+Object defaultCriterion()
 * }
 * Event <|-- UserEventType
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jgrapes.core;
