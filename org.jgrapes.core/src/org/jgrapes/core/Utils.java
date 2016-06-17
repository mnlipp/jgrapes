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

import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;
import org.jgrapes.core.internal.ComponentNode;
import org.jgrapes.core.internal.GeneratorRegistry;

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
		manager(application).fire(new Start() , Channel.BROADCAST).get();
	}

	/**
	 * Wait until all generators and event queues are exhausted. When this
	 * stage is reached, nothing can happen anymore unless a new event is
	 * sent from an external thread.
	 * 
	 * @throws InterruptedException 
	 */
	public static void awaitExhaustion() throws InterruptedException {
		GeneratorRegistry.getInstance().awaitExhaustion();
	}

}
