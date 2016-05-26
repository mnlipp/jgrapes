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

import java.util.List;

/**
 * Provides methods for manipulating the
 * component hierarchy and for firing events. Every component has
 * access to a manager implementation that manages the component.
 * <P>
 * The <code>Manager</code> for a component that extends from 
 * {@link AbstractComponent} is provided by the base class itself.
 * Components that only implement the {@link Component} interface
 * get an associated <code>Manager</code> assigned to their annotated 
 * attribute.
 * 
 * @author Michael N. Lipp
 * @see Component
 */
public interface Manager extends Iterable<Component> {

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
	Component detach ();

	/**
	 * Attaches the given component node as a child to the component
	 * managed by this manager.
	 * 
	 * @param child the component to add
	 * @return the component's manager (for comfortable chaining)
	 */
	Manager attach (Component child);
	
	/**
	 * Returns the child components of the component managed by 
	 * this manager as unmodifiable list.
	 * 
	 * @return the child components
	 */
	List<Component> getChildren();

	/**
	 * Returns the parent of the component managed by this manager.
	 * 
	 * @return the parent component or <code>null</code> if the
	 * component is not registered with another component
	 */
	Component getParent();
	
	/**
	 * Returns the root of the tree the component 
	 * managed by this manager belongs to.
	 * 
	 * @return the root
	 */
	Component getRoot();
	
	/**
	 * Returns the channel if the component managed by this manager.
	 * 
	 * @return the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 */
	Channel getChannel();
	
	/**
	 * Fires the given event on the given channel. If no channels are
	 * specified as parameters, the event is fired on the event's 
	 * channel (see {@link Event#getChannels()}). If the event doesn't
	 * specify channels either, the event is fired on the 
	 * channel of the component managed by this manager 
	 * (see {@link #getChannel()}). As last resort, the
	 * event is fired on the broadcast channel.
	 * <P>
	 * If an event is fired inside an event handler, it is added to the
	 * {@link EventPipeline} that has invoked the handler. If an event is fired
	 * by some other thread (not associated with a pipeline), a new pipeline
	 * is created for handling the event (and any events triggered by it). 
	 * 
	 * @param event the event to fire
	 * @param channels the channels to fire the event on
	 */
	void fire(Event event, Channel... channels);

	/**
	 * Adds a method of the component managed by this manager as a 
	 * handler for a specific event and channel. The method
	 * with the given name must have a single argument of type
	 * {@link Event} (or a derived type as appropriate for the
	 * event type to be handled).
	 * 
	 * @param eventKey the event key that should be used for matching
	 * this handler with an event. This is equivalent to an 
	 * <code>events</code>/<code>namedEvents</code> parameter
	 * used with a single value in the handler annotation, but here 
	 * all kinds of Objects are allowed as key values.
	 * @param channelKey the channel key that should be used for matching
	 * this handler with a channel. This is equivalent to a 
	 * <code>channels</code>/<code>namedChannels</code> parameter
	 * used with a single value in the handler annotation, but here 
	 * all kinds of Objects are allowed as key values. If the
	 * actual object provided is a {@link Channel}, its
	 * match key is used for matching.
	 * @param method the name of the method that implements the handler
	 * @param priority the priority of the handler
	 */
	void addHandler(Object eventKey, Object channelKey, 
			String method, int priority);
	
	/**
	 * A shortcut for invoking {@link #addHandler(Object, Object, String, int)}
	 * with priority 0.
	 */
	void addHandler(Object eventKey, Object channelKey,	String method);
	
	/**
	 * Return a new {@link EventPipeline} that processes the added events
	 * using a thread from a thread pool.
	 * 
	 * @return the pipeline
	 */
	EventPipeline newEventPipeline();
	
	/**
	 * Return a new {@link EventPipeline} that processes an added event
	 * (and all events caused by it) before returning from the
	 * {@link EventPipeline#add} method.
	 * <P>
	 * The returned event pipeline is not thread-safe, i.e. no other thread
	 * may call <code>add</code> while an invocation of <code>add</code>
	 * is being processed.
	 * 
	 * @return the pipeline
	 */
	EventPipeline newSyncEventPipeline();
}
