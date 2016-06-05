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

import org.jgrapes.core.events.AbstractCompletedEvent;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;
import org.jgrapes.core.internal.ComponentNode;

/**
 * This class provides some utility functions.
 * 
 * @author Michael N. Lipp
 */
public class Utils {

	private Utils() {
	}
	
	/**
	 * Returns a component's manager. For a component that inherits
	 * from {@link org.jgrapes.core.AbstractComponent} this method simply returns
	 * the component as it is its own manager.
	 * 
	 * For components that implement {@link Component} but don't inherit from 
	 * {@link org.jgrapes.core.AbstractComponent} the method returns the value of 
	 * the attribute annotated as manager slot. If the attribute is still
	 * empty, this method makes the component the root
	 * of a new tree and returns its manager.
	 * 
	 * @param component the component
	 * @return the component (with its manager attribute set)
	 */
	public static Manager manager (Component component) {
		return ComponentNode.getComponentNode(component);
	}

	/**
	 * Fires a {@link Start} event with an associated
	 * {@link Started} completion event on the broadcast channel
	 * of the given application and wait for the completion of the
	 * <code>Start</code> event.
	 * 
	 * @param application the application to start
	 */
	public static void start(Component application) 
			throws InterruptedException {
		fireAndAwait(application, new Start(), Channel.BROADCAST);
	}

	/**
	 * Fire the given event from the given component (usually the application)
	 * and await its completion.
	 * 
	 * @param application the component (used to get the channels if none
	 * are specified in the event or as parameters)
	 * @param event the event
	 * @param channels the channels on which the event is fired
	 * @return the event (for easy method chaining
	 * @throws InterruptedException if waiting is interrupted
	 * @see Manager#fire(Event, Channel...)
	 */
	public static Event fireAndAwait
			(Component application, Event event, Channel... channels) 
			throws InterruptedException {
		manager(application).fire(event , channels);
		event.awaitCompleted();
		return event;
	}
	
}
