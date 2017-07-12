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

package org.jgrapes.core.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.jgrapes.core.Associator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;

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

	/** The channels that this event is to be fired on if no
	 * channels are specified explicitly when firing. */
	protected Channel[] channels = null;
	/** The event that caused this event. */
	private EventBase<?> generatedBy = null;
	/** Number of events that have to be dispatched until completion.
	 * This is one for the event itself and one more for each event
	 * that has this event as its cause. */
	private AtomicInteger openCount = new AtomicInteger(0);
	/** The event to be fired upon completion. */
	private Set<Event<?>> completionEvents = null;
	/** Set when the event has been completed. */
	private boolean completed = false;
	/** Set when the event is enqueued, reset when it has been completed. */
	private EventProcessor processedBy = null;
	/** Indicates that the event should not processed further. */
	private boolean stopped = false;
	private boolean cancelled = false;
	/** The results of handling the event (if any). */
	private List<T> results;
	/** Context data. */
	private Map<Object,Object> contextData = null;
	
	/**
	 * Returns the channels associated with the event. Before an
	 * event has been fired, this returns the channels set with
	 * {@link #setChannels(Channel[])}. After an event has been
	 * fired, this returns the channels that the event has
	 * effectively been fired on 
	 * (see {@link Manager#fire(Event, Channel...)}).
	 * 
	 * @return the channels
	 */
	public Channel[] channels() {
		return channels;
	}
	
	/**
	 * Returns the subset of channels that are assignable to the given type.
	 * 
	 * @param <C> the given type's class
	 * @param type the class to look for
	 * @return the filtered channels
	 * @see #channels()
	 */
	@SuppressWarnings("unchecked")
	public <C> C[] channels(Class<C> type) {
		return Arrays.stream(channels)
		        .filter(c -> type.isAssignableFrom(c.getClass())).toArray(
		        		size -> (C[])Array.newInstance(type, size));
	}

	/**
	 * Execute the given handler for all channels of the given type.
	 * 
	 * @param <E> the type of the event
	 * @param <C> the type of the channel
	 * @param type the channel type
	 * @param handler the handler
	 */
	@SuppressWarnings("unchecked")
	public <E extends EventBase<?>, C extends Channel> void forChannels(
			Class<C> type, BiConsumer<E, C> handler) {
		Arrays.stream(channels)
		        .filter(c -> type.isAssignableFrom(c.getClass()))
		        .forEach(c -> handler.accept((E)this, (C)c));
	}
	
	/**
	 * Sets the channels that the event is fired on if no channels
	 * are specified explicitly when firing the event
	 * (see {@link org.jgrapes.core.Manager#fire(Event, Channel...)}).
	 * 
	 * @param channels the channels to set
	 * @return the object for easy chaining
	 * 
	 * @throws IllegalStateException if the method is called after
	 * this event has been fired
	 */
	public Event<T> setChannels(Channel... channels) {
		if (enqueued()) {
			throw new IllegalStateException(
					"Channels cannot be changed after fire");
		}
		this.channels = channels;
		return (Event<T>)this;
	}

	/**
	 * Returns <code>true</code> if the event has been enqueued in a pipeline.
	 * 
	 * @return the result
	 */
	protected boolean enqueued() {
		return openCount.get() > 0;
	}

	/**
	 * Sets the result of handling this event. If this method is invoked 
	 * more then once, the various results are collected in a list. This
	 * can happen if the event is handled by several components. 
	 * 
	 * @param result the result to set
	 * @return the object for easy chaining
	 */
	public synchronized Event<T> setResult(T result) {
		if (results == null) {
			results = new ArrayList<T>();
		}
		results.add(result);
		return (Event<T>)this;
	}

	/**
	 * Allows access to the intermediate result before the 
	 * completion of the event. 
	 * 
	 * @return the intermediate results or `null`
	 */
	protected List<T> currentResults() {
		return Collections.unmodifiableList(results);
	}
	
	/**
	 * Tie the result of this event to the result of the other event.
	 * Changes of either event's results will subsequently be applied
	 * to both events.
	 * <P>
	 * This is useful when an event is replaced by another event during
	 * handling like:
	 * {@code fire((new Event()).tieTo(oldEvent.stop()))}  
	 * 
	 * @param other the event to tie to
	 * @return the object for easy chaining
	 */
	public synchronized Event<T> tieTo(EventBase<T> other) {
		if (other.results == null) {
			other.results = new ArrayList<T>();
		}
		results = other.results;
		return (Event<T>)this;
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
	void decrementOpen(InternalEventPipeline pipeline) {
		if (openCount.decrementAndGet() == 0 && !completed) {
			synchronized (this) {
				completed = true;
				notifyAll();
			}
			if (completionEvents != null && !cancelled) {
				for (Event<?> e: completionEvents) {
					Channel[] completeChannels = e.channels();
					if (completeChannels == null) {
						// Note that channels cannot be null, as it is set
						// when firing the event and an event is never fired
						// on no channels.
						completeChannels = channels;
						e.setChannels(completeChannels);
					}
					processedBy.add(e, completeChannels);
				}
			}
			if (generatedBy != null) {
				generatedBy.decrementOpen(pipeline);
			}
			processedBy = null; // No longer needed
		}
	}

	/**
	 * Returns the events to be thrown when this event and all events caused
	 * by it have been handled.
	 * 
	 * @return the completed events
	 */
	public Set<Event<?>> completionEvents() {
		return completionEvents == null ? Collections.emptySet()
				: Collections.unmodifiableSet(completionEvents);
	}

	/**
	 * Adds the given event to the events to be thrown when this event 
	 * and all events caused by it have been handled. 
	 * 
	 * The completion events handled by the same {@link EventProcessor}
	 * as the event that has been completed.
	 * 
	 * @param completionEvent the completion event to add
	 * @return the object for easy chaining
	 */
	public Event<T> addCompletionEvent(Event<?> completionEvent) {
		if (completionEvents == null) {
			completionEvents = new HashSet<>();
		}
		completionEvents.add(completionEvent);
		return (Event<T>)this;
	}

	/**
	 * Check if this event has been completed.
	 * 
	 * @return the completed state
	 */
	@Override
	public boolean isDone() {
		return completed;
	}

	/**
	 * Invoked after all handlers for the event have been executed. 
	 * May be overridden by derived classes to cause some immediate effect
	 * (instead of e.g. waiting for the completion event). The default 
	 * implementation does nothing. This method is invoked by the event 
	 * handler thread and must not block.
	 */
	protected void handled() {
	}
	
	/**
	 * Prevents the invocation of further handlers (like {@link #stop()} 
	 * and (in addition) the invocation of any added completed events.
	 * 
	 * @param mayInterruptIfRunning ignored
	 * @return `false` if the event has already been completed
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (!completed && !cancelled) {
			stop();
			cancelled = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Waits for the event to be completed and returns the first (or only)
	 * result.
	 * 
	 * @see Future#get()
	 */
	@Override
	public T get() throws InterruptedException {
		while (true) {
			synchronized(this) {
				if (completed) {
					return ((results == null || results.isEmpty()))
							? null : results.get(0);
				}
				wait();
			}
		}
	}

	/**
	 * Waits for the event to be completed and returns the list
	 * of results (which may be empty).
	 * 
	 * @return the results
	 * @see Future#get()
	 */
	public List<T> results() throws InterruptedException {
		while (true) {
			synchronized(this) {
				if (completed) {
					return (results == null ? Collections.emptyList() 
							: Collections.unmodifiableList(results));
				}
				wait();
			}
		}
	}

	/**
	 * Causes the invoking thread to wait until the processing of the 
	 * event has been completed or given timeout has expired and returns
	 * the first (or only) result. 
	 * 
	 * @return the result
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public T get(long timeout, TimeUnit unit)
	        throws InterruptedException, TimeoutException {
		synchronized(this) {
			if (completed) {
				return (results == null || results.isEmpty())
						? null : results.get(0);
			}
			wait(unit.toMillis(timeout));
		}
		if (completed) {
			return (results == null || results.isEmpty())
					? null : results.get(0);
		}
		throw new TimeoutException();
	}

	/**
	 * Causes the invoking thread to wait until the processing of the 
	 * event has been completed or given timeout has expired and returns
	 * the list of results (which may be empty). 
	 * 
	 * @return the results
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	public List<T> results(long timeout, TimeUnit unit)
	        throws InterruptedException, TimeoutException {
		synchronized(this) {
			if (completed) {
				return (results == null ? Collections.emptyList() 
						: Collections.unmodifiableList(results));
			}
			wait(unit.toMillis(timeout));
		}
		if (completed) {
			return (results == null ? Collections.emptyList() 
					: Collections.unmodifiableList(results));
		}
		throw new TimeoutException();
	}

	@Override
	public Event<T> setAssociated(Object by, Object with) {
		if (contextData == null) {
			contextData = new ConcurrentHashMap<>();
		}
		contextData.put(by, with);
		return (Event<T>)this;
	}

	@Override
	public <V> Optional<V> associated(Object by, Class<V> type) {
		if (contextData == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(type.cast(contextData.get(by)));
	}
	
}
