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

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.jgrapes.core.annotation.ComponentManager;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;
import org.jgrapes.core.internal.Common;
import org.jgrapes.core.internal.ComponentVertex;
import org.jgrapes.core.internal.GeneratorRegistry;

/**
 * This class provides some utility functions.
 */
public class Components {

	private Components() {
	}
	
	private static ExecutorService defaultExecutorService 
		= Executors.newCachedThreadPool();

	private static ExecutorService timerExecutorService
		= defaultExecutorService;
	
	/**
	 * Return the default executor service for the framework.
	 * 
	 * @return the defaultExecutorService
	 */
	public static ExecutorService defaultExecutorService() {
		return defaultExecutorService;
	}

	/**
	 * Set the default executor service for the framework. The default 
	 * value is a cached thread pool (see @link 
	 * {@link Executors#newCachedThreadPool()}).
	 * 
	 * @param defaultExecutorService the executor service to set
	 */
	public static void setDefaultExecutorService(
	        ExecutorService defaultExecutorService) {
		// If the timer executor service is set to the default
		// executor service, adjust it to the new value as well.
		if (timerExecutorService == Components.defaultExecutorService) {
			timerExecutorService = defaultExecutorService;
		}
		Components.defaultExecutorService = defaultExecutorService;
	}

	/**
	 * Returns a component's manager. For a component that inherits
	 * from {@link org.jgrapes.core.Component} this method simply returns
	 * the component as it is its own manager.
	 * 
	 * For components that implement {@link ComponentType} but don't inherit from 
	 * {@link org.jgrapes.core.Component} the method returns the value of 
	 * the attribute annotated as manager slot. If this attribute is still
	 * empty, this method makes the component the root
	 * of a new tree and returns its manager.
	 * 
	 * @param component the component
	 * @return the component (with its manager attribute set)
	 */
	public static Manager manager(ComponentType component) {
		return ComponentVertex.componentVertex(component, null);
	}

	/**
	 * Returns a component's manager like {@link #manager(ComponentType)}.
	 * If the manager slot attribute is empty, the component is initialized
	 * with its component channel set to the given parameter. Invoking
	 * this method overrides any channel set in the
	 * {@link ComponentManager} annotation.
	 * 
	 * This method is usually invoked by the constructor of a class
	 * that implements {@link ComponentType}.
	 * 
	 * @param component the component
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event, Channel...)} sends the event to 
	 * @return the component (with its manager attribute set)
	 * @see Component#Component(Channel)
	 */
	public static Manager manager(
			ComponentType component, Channel componentChannel) {
		return ComponentVertex.componentVertex(component, componentChannel);
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
	public static void start(ComponentType application) 
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
		GeneratorRegistry.instance().awaitExhaustion();
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
		return GeneratorRegistry.instance().awaitExhaustion(timeout);
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
	 * Returns the full name of the object's class together with an id (see 
	 * {@link #objectId(Object)}). The result can be used as a unique
	 * human readable identifier for arbitrary objects.
	 * 
	 * @param object
	 *            the object
	 * @return the object's name
	 */
	public static String objectFullName(Object object) {
		StringBuilder builder = new StringBuilder();
		builder.append(object.getClass().getName());
		builder.append('#');
		builder.append(objectId(object));
		return builder.toString();
	}

	/**
	 * Returns the name of the object's class together with an id (see 
	 * {@link #objectId(Object)}). May be used to implement {@code toString()}
	 * with identifiable objects. If the log level is "finer", the full
	 * class name will be used for the returned value, else the simple name.
	 * 
	 * @param object
	 *            the object
	 * @return the object's name
	 */
	public static String objectName(Object object) {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.classToString(object.getClass()));
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
			return objectIds.computeIfAbsent(object, k -> 
				Long.toString(idCounters
						.computeIfAbsent(scope, l -> new AtomicLong())
							.incrementAndGet()));
			
		}
	}

	/**
	 * Returns the full name or simple name of the class depending
	 * on the log level.
	 * 
	 * @param clazz the class
	 * @return the name
	 */
	public static String classToString(Class<?> clazz) {
		if (Common.classNames.isLoggable(Level.FINER)) {
			return clazz.getName();
		} else {
			return clazz.getSimpleName();
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
	 * id (see {@link Components#objectId(Object)}).
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

	/**
	 * Instances are added to the scheduler in order to be invoked
	 * at a given time.
	 */
	@FunctionalInterface
	public static interface TimeoutHandler {

		/**
		 * Invoked when the timeout occurs.
		 * 
		 * @param timer the timer that has timed out and needs handling
		 */
		void timeout(Timer timer);
	}

	/**
	 * Represents a timer as created by 
	 * {@link Components#schedule(TimeoutHandler, Instant)}.
	 */
	public static class Timer {
		private Scheduler scheduler;
		private TimeoutHandler timeoutHandler;
		private Instant scheduledFor;

		private Timer(Scheduler scheduler,
				TimeoutHandler timeoutHandler, Instant scheduledFor) {
			this.scheduler = scheduler;
			this.timeoutHandler = timeoutHandler;
			this.scheduledFor = scheduledFor;
		}

		/**
		 * Reschedules the timer for the given instant.
		 * 
		 * @param scheduledFor the instant
		 */
		public void reschedule(Instant scheduledFor) {
			scheduler.reschedule(this, scheduledFor);
		}

		/**
		 * Reschedules the timer for the given duration after now.
		 * 
		 * @param scheduledFor the timeout
		 */
		public void reschedule(Duration scheduledFor) {
			reschedule(Instant.now().plus(scheduledFor));			
		}
		
		/**
		 * Returns the timeout handler of this timer.
		 * 
		 * @return the handler
		 */
		public TimeoutHandler timeoutHandler() {
			return timeoutHandler;
		}

		/**
		 * Returns the instant that this handler is scheduled for.
		 * 
		 * @return the instant
		 */
		public Instant scheduledFor() {
			return scheduledFor;
		}

		/**
		 * Cancels this timer.
		 */
		public void cancel() {
			scheduler.cancel(this);
		}
	}

	/**
	 * Returns the executor service used for executing timers.
	 * 
	 * @return the timer executor service
	 */
	public static ExecutorService timerExecutorService() {
		return timerExecutorService;
	}

	/**
	 * Sets the executor service used for executing timers.
	 * Defaults to the {@link #defaultExecutorService()}.
	 * 
	 * @param timerExecutorService the timerExecutorService to set
	 */
	public static void setTimerExecutorService(
	        ExecutorService timerExecutorService) {
		Components.timerExecutorService = timerExecutorService;
	}

	/**
	 * A general purpose scheduler.
	 */
	private static class Scheduler extends Thread {

		private PriorityQueue<Timer> timers 
			= new PriorityQueue<>(10,
					Comparator.comparing(Timer::scheduledFor)); 

		public Scheduler() {
			setName("Components.Scheduler");
			setDaemon(true);
			start();
		}
		
		public Timer schedule(
				TimeoutHandler timeoutHandler, Instant scheduledFor) {
			Timer timer = new Timer(this, timeoutHandler, scheduledFor);
			synchronized (this) {
				timers.add(timer);
				notify();
			}
			return timer;
		}

		private void reschedule(Timer timer, Instant scheduledFor) {
			synchronized (this) {
				timers.remove(timer);
				timer.scheduledFor = scheduledFor;
				timers.add(timer);
				notify();
			}
		}
		
		private void cancel(Timer timer) {
			synchronized (this) {
				timers.remove(timer);
				notify();
			}
		}
		
		@Override
		public void run() {
			while (true) {
				Instant now = Instant.now();
				synchronized (this) {
					while (true) {
						final Timer first = timers.peek();
						if (first == null || first.scheduledFor().isAfter(now)) {
							break;
						}
						timers.poll();
						timerExecutorService.submit( 
								() -> first.timeoutHandler().timeout(first));
					}
					try {
						if (timers.size() == 0) {
							wait();
						} else {
							wait(Math.max(1, Duration.between(now, 
									timers.peek().scheduledFor()).toMillis()));
						}
					} catch (Throwable e) {
					}
				}
			}
		}
	}
	
	private static Scheduler scheduler = new Scheduler();

	/**
	 * Schedules the given timeout handler for the given instance. 
	 * 
	 * @param timeoutHandler the handler
	 * @param scheduledFor the instance in time
	 * @return the timer
	 */
	public static Timer schedule(
			TimeoutHandler timeoutHandler, Instant scheduledFor) {
		return scheduler.schedule(timeoutHandler, scheduledFor);
	}

	/**
	 * Schedules the given timeout handler for the given 
	 * offset from now. 
	 * 
	 * @param timeoutHandler the handler
	 * @param scheduledFor the time to wait
	 * @return the timer
	 */
	public static Timer schedule(
			TimeoutHandler timeoutHandler, Duration scheduledFor) {
		return scheduler.schedule(
				timeoutHandler, Instant.now().plus(scheduledFor));
	}
}
