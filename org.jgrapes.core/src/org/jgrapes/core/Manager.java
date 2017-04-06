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

package org.jgrapes.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * The interface that provides the methods for manipulating the
 * component hierarchy and for firing events. Every component has
 * access to a manager implementation that manages the component.
 * <P>
 * The <code>Manager</code> for a component that extends from 
 * {@link Component} is provided by the base class itself.
 * Components that only implement the {@link ComponentType} interface
 * get an associated <code>Manager</code> assigned to their annotated 
 * attribute.
 * 
 * @author Michael N. Lipp
 * @see ComponentType
 */
public interface Manager extends Iterable<ComponentType> {

	/**
	 * Detaches the component managed by this manager (with its children,
	 * if any) from the component tree that it currently belongs to.
	 * <P>
	 * This method results in a <code>IllegalStateException</code> if
	 * called on a tree before a {@link org.jgrapes.core.events.Start} 
	 * event has been fired on 
	 * it. The Reason for this restriction is that distributing
	 * buffered events between the two separated trees cannot easily be
	 * defined in an intuitive way.
	 * 
	 * @return the component (for comfortable chaining)
	 * @throws IllegalStateException if invoked before a <code>Start</code>
	 * event
	 */
	ComponentType detach();

	/**
	 * Attaches the given component node (or complete tree) as a child 
	 * to the component managed by this manager. The node or tree may not
	 * have been started.
	 * 
	 * @param <T> the component node's type
	 * @param child the component to add
	 * @return the added component (for comfortable chaining)
	 */
	<T extends ComponentType> T attach(T child);
	
	/**
	 * Returns the child components of the component managed by 
	 * this manager as unmodifiable list.
	 * 
	 * @return the child components
	 */
	List<ComponentType> children();

	/**
	 * Returns the parent of the component managed by this manager.
	 * 
	 * @return the parent component or <code>null</code> if the
	 * component is not registered with another component
	 */
	ComponentType parent();
	
	/**
	 * Returns the root of the tree the component 
	 * managed by this manager belongs to.
	 * 
	 * @return the root
	 */
	ComponentType root();
	
	/**
	 * Returns the channel of the component managed by this manager.
	 * 
	 * @return the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	Channel channel();
	
	/**
	 * Fires the given event on the given channel. If no channels are
	 * specified as parameters, the event is fired on the event's 
	 * channel (see {@link Event#channels()}). If the event doesn't
	 * specify channels either, the event is fired on the 
	 * channel of the component managed by this manager 
	 * (see {@link #channel()}).
	 * <P>
	 * If an event is fired inside an event handler, it is added to the
	 * {@link EventPipeline} that has invoked the handler. If an event is fired
	 * by some other thread (not associated with a pipeline), a new pipeline
	 * is created for handling the event (and any events triggered by it). 
	 * 
	 * @param <T> the result type of the event
	 * @param event the event to fire
	 * @param channels the channels to fire the event on
	 * @return the event (for easy chaining)
	 */
	<T> Event<T> fire(Event<T> event, Channel... channels);

	/**
	 * Adds a handler for the given mathod with the given scope
	 * and priority.
	 * 
	 * @param method the method to invoke 
	 * @param scope the handler scope to be used for matching events
	 * @param priority the priority of the handler
	 */
	void addHandler(Method method, HandlerScope scope, int priority);
	
	/**
	 * Returns the pipeline used when firing an event.
	 * 
	 * @return the event pipeline
	 * @see #fire(Event, Channel...) 
	 */
	EventPipeline activeEventPipeline();
	
	/**
	 * Return a new {@link EventPipeline} that processes the added events
	 * using a thread from a thread pool.
	 * 
	 * @return the pipeline
	 */
	EventPipeline newEventPipeline();
	
	/**
	 * Return a new {@link EventPipeline} that processes the added events
	 * using threads from the given executor service.
	 * 
	 * @param executorService the executor service
	 * @return the pipeline
	 */
	EventPipeline newEventPipeline(ExecutorService executorService);
	
	/**
	 * Return a new {@link EventPipeline} that processes an added event
	 * (and all events caused by it) before returning from the
	 * {@link EventPipeline#fire} method.
	 * <P>
	 * The returned event pipeline is not thread-safe, i.e. no other thread
	 * may call <code>add</code> while an invocation of <code>add</code>
	 * is being processed.
	 * 
	 * @return the pipeline
	 */
	EventPipeline newSyncEventPipeline();

	/**
	 * Register the managed component as a running generator. 
	 */
	void registerAsGenerator();

	/**
	 * Unregister the managed component as a running generator.
	 */
	void unregisterAsGenerator();
}
