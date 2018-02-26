/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.core.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapes.core.Associator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionLock;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * Provides the implementations of methods to class {@link Event} that
 * need access to classes or methods that are visible in the implementation
 * package only. The class is not intended to be used as base class
 * for any other class.
 * 
 * @param <T> the result type of the event. Use {@link Void} if handling
 * the event does not produce a result
 */
public abstract class EventBase<T> 
	implements Eligible, Future<T>, Associator {

	/** The event that caused this event. */
	private EventBase<?> generatedBy = null;
	/** Number of events that have to be dispatched until completion.
	 * This is one for the event itself and one more for each event
	 * that has this event as its cause. */
	private AtomicInteger openCount = new AtomicInteger(0);
	/** Completion locks. */
	private Set<CompletionLockBase> completionLocks = null;
	/** Set when the event is enqueued, reset when it has been completed. */
	private EventProcessor processedBy = null;
	/** Indicates that the event should not processed further. */
	private boolean stopped = false;
	/** The events to be fired upon completion. Using this attribute
	 * provides a slightly faster access than invoking
	 * {@link Event#completionEvents()}, which wraps the result in
	 * an unmodifiable set. */
	protected Set<Event<?>> completionEvents = null;
	/** Set when the event has been completed. */
	protected boolean completed = false;
	
	public abstract Channel[] channels();

	protected abstract void handled();

	protected abstract List<T> currentResults();
	
	/**
	 * Returns <code>true</code> if the event has been enqueued in a pipeline.
	 * 
	 * @return the result
	 */
	protected boolean enqueued() {
		return processedBy != null || completed || isCancelled();
	}

	/**
	 * Invoked when an exception occurs while invoking a handler for an event.
	 * 
	 * @param eventProcessor the manager that has invoked the handler
	 * @param throwable the exception that has been thrown by the handler
	 */
	protected abstract void handlingError(
			EventPipeline eventProcessor, Throwable throwable);

	/**
	 * Can be called during the execution of an event handler to indicate
	 * that the event should not be processed further. All remaining 
	 * handlers for this event will be skipped.
	 * 
	 * @return the object for easy chaining
	 */
	public Event<T> stop() {
		stopped = true;
		return (Event<T>)this;
	}

	/**
	 * Returns <code>true</code> if {@link #stop} has been called.
	 * 
	 * @return the stopped state
	 */
	public boolean isStopped() {
		return stopped;
	}

	/**
	 * If an event is fired while processing another event, note
	 * the event being processed. This allows us to track the cause
	 * of events to the "initial" (externally) generated event that
	 * triggered everything.
	 * 
	 * @param causingEvent the causing event to set
	 */
	void generatedBy(EventBase<?> causingEvent) {
		openCount.incrementAndGet();
		generatedBy = causingEvent;
		if (causingEvent != null) {
			causingEvent.openCount.incrementAndGet();
		}
	}

	/**
	 * Set the processor that will (eventually) process the event. 
	 * 
	 * @param processor the processor
	 */
	void processedBy(EventProcessor processor) {
		this.processedBy = processor;
	}
	
	/**
	 * @param pipeline
	 */
	void decrementOpen() {
		if (openCount.decrementAndGet() == 0 && !completed) {
			synchronized (this) {
				completed = true;
				notifyAll();
			}
			if (completionEvents != null && !isCancelled()) {
				processedBy.updateNewEventsParent(generatedBy);
				for (Event<?> e: completionEvents) {
					Channel[] completeChannels = e.channels();
					if (completeChannels == null) {
						// Note that channels cannot be null, as it is set
						// when firing the event and an event is never fired
						// on no channels.
						completeChannels = channels();
						e.setChannels(completeChannels);
					}
					processedBy.add(e, completeChannels);
				}
			}
			if (generatedBy != null) {
				generatedBy.decrementOpen();
			}
			processedBy = null; // No longer needed
		}
	}

	/**
	 * Adds the given completion lock. 
	 * 
	 * @param lock the lock
	 * @see CompletionLock
	 */
	Event<T> addCompletionLock(CompletionLockBase lock) {
		synchronized (this) {
			if (completionLocks == null) {
				completionLocks = Collections.synchronizedSet(new HashSet<>());
			}
		}
		if (completionLocks.add(lock)) {
			openCount.incrementAndGet();
			lock.startTimer();
		}
		return (Event<T>)this;
	}

	/**
	 * Removes the given completion lock. 
	 * 
	 * @param lock the lock
	 * @see CompletionLock
	 */
	void removeCompletionLock(CompletionLockBase lock) {
		if (completionLocks == null) {
			return;
		}
		if (completionLocks.remove(lock)) {
			decrementOpen();
		}
		lock.cancelTimer();
	}
	
}
