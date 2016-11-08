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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;
import org.jgrapes.core.internal.Common;
import org.jgrapes.core.internal.ComponentVertex;
import org.jgrapes.core.internal.GeneratorRegistry;

/**
 * This class provides some utility functions.
 * 
 * @author Michael N. Lipp
 */
public class Components {

	private Components() {
	}
	
	/**
	 * Returns a component's manager. For a component that inherits
	 * from {@link org.jgrapes.core.Component} this method simply returns
	 * the component as it is its own manager.
	 * 
	 * For components that implement {@link AttachedComponent} but don't inherit from 
	 * {@link org.jgrapes.core.Component} the method returns the value of 
	 * the attribute annotated as manager slot. If the attribute is still
	 * empty, this method makes the component the root
	 * of a new tree and returns its manager.
	 * 
	 * @param component the component
	 * @return the component (with its manager attribute set)
	 */
	public static Manager manager (AttachedComponent component) {
		return ComponentVertex.getComponentVertex(component);
	}

	/**
	 * Fires a {@link Start} event with an associated
	 * {@link Started} completion event on the broadcast channel
	 * of the given application and wait for the completion of the
	 * <code>Start</code> event.
	 * 
	 * @param application the application to start
	 * @throws InterruptedException if the execution was interrupted
	 */
	public static void start(AttachedComponent application) 
			throws InterruptedException {
		manager(application).fire(new Start() , Channel.BROADCAST).get();
	}

	/**
	 * Wait until all generators and event queues are exhausted. When this
	 * stage is reached, nothing can happen anymore unless a new event is
	 * sent from an external thread.
	 * 
	 * @throws InterruptedException if the current thread was interrupted
	 * while waiting
	 */
	public static void awaitExhaustion() throws InterruptedException {
		GeneratorRegistry.getInstance().awaitExhaustion();
	}

	/**
	 * Wait until all generators and event queues are exhausted or
	 * the maximum wait time has expired.
	 * 
	 * @param timeout the wait time in milliseconds
	 * @return {@code true} if exhaustion state was reached
	 * @throws InterruptedException if the execution was interrupted 
	 * @see #awaitExhaustion()
	 */
	public static boolean awaitExhaustion(long timeout) 
			throws InterruptedException {
		return GeneratorRegistry.getInstance().awaitExhaustion(timeout);
	}

	/**
	 * Utility method that checks if an assertion error has occurred
	 * while executing handlers. If so, the error is thrown and
	 * the assertion error store is reset.
	 * <P>
	 * This method is intended for junit tests. It enables easy propagation
	 * of assertion failures to the main thread.
	 * 
	 * @throws AssertionError if an assertion error occurred while
	 * executing the application
	 */
	public static void checkAssertions() {
		Common.checkAssertions();
	}
	
	/**
	 * Returns the class of the object together with an id (see @link
	 * {@link #objectId(Object)}). May be used to implement {@code toString()}
	 * with identifiable objects.
	 * 
	 * @param object
	 *            the object
	 * @return the object's name
	 */
	public static String objectName(Object object) {
		StringBuilder builder = new StringBuilder();
		builder.append(Common.classToString(object.getClass()));
		builder.append('#');
		builder.append(objectId(object));
		return builder.toString();
	}

	private static Map<Object, String> objectIds = new WeakHashMap<>();
	private static Map<Class<?>, AtomicLong> idCounters = new WeakHashMap<>();

	private static String getId(Class<?> scope, Object object) {
		if (object == null) {
			return "?";
		}
		synchronized (objectIds) {
			return objectIds.computeIfAbsent
				(object, k -> Long.toString
					(idCounters.computeIfAbsent(scope, l -> new AtomicLong())
							.incrementAndGet()));
			
		}
	}

	/**
	 * Returns an id of the object that is unique within a specific scope. Ids
	 * are generated and looked up in the scope of the object's class unless the
	 * class implements {@link IdInfoProvider}.
	 * 
	 * @param object
	 *            the object
	 * @return the object's name
	 */
	public static String objectId(Object object) {
		if (object instanceof IdInfoProvider) {
			return getId(((IdInfoProvider) object).idScope(),
			        ((IdInfoProvider) object).idObject());
		} else {
			return getId(object.getClass(), object);
		}
	}

	/**
	 * Implemented by classes that want a special class (scope) to be used
	 * for looking up their id or want to map to another object for getting the
	 * id.
	 * 
	 * @author Michael N. Lipp
	 */
	public static interface IdInfoProvider {
		
		/**
		 * Returns the scope.
		 * 
		 * @return the scope
		 */
		Class<?> idScope();
		
		/**
		 * Returns the object to be used for generating the id.
		 * 
		 * @return the object (defaults to {@code this})
		 */
		default Object idObject() {
			return this;
		}
	}
}
