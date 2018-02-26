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

package org.jgrapes.core;

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
import java.util.function.BiConsumer;

import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.internal.EventBase;
import org.jgrapes.core.internal.EventProcessor;

/**
 * This class is the base class for all events.
 *  
 * By default (i.e. as implemented by this class), the event's kind is
 * represented by its Java class and the eligibility is based on
 * the "is a" relationship between classes. An event is eligible if its class
 * is equal to or a super class of the class used as criterion. 
 * This default behavior can be changed by overriding the
 * methods from {@link Eligible}. See {@link NamedEvent} as an example.
 * 
 * @param <T>
 *            the result type of the event. Use {@link Void} if handling the
 *            event does not produce a result
 */
public class Event<T> extends EventBase<T> {

	/** The channels that this event is to be fired on if no
	 * channels are specified explicitly when firing. */
	private Channel[] channels = null;
	/** The results of handling the event (if any). */
	private List<T> results;
	/** Context data. */
	private Map<Object,Object> contextData = null;
	private boolean cancelled = false;
	
	/**
	 * Creates a new event. Passing channels is equivalent to first
	 * creating the event and then calling {@link #setChannels(Channel...)}
	 * with the given channels.
	 * 
	 * @param channels the channels to set
	 */
	public Event(Channel... channels) {
		super();
		if (channels.length > 0) {
			setChannels(channels);
		}
	}

	/**
	 * Returns the class of this event as representation of its kind.
	 * 
	 * @return the class of this event
	 * 
	 * @see org.jgrapes.core.Eligible#defaultCriterion()
	 */
	@Override
	public Object defaultCriterion() {
		return getClass();
	}

	/**
	 * Returns <code>true</code> if the `criterion` 
	 * is of the same class or a base class of this event's class.
	 * 
	 * @see org.jgrapes.core.Eligible#isEligibleFor(java.lang.Object)
	 */
	@Override
	public boolean isEligibleFor(Object criterion) {
		return Class.class.isInstance(criterion)
				&& ((Class<?>)criterion).isAssignableFrom(getClass());
	}

	/**
	 * Implements the default behavior for handling events thrown
	 * by a handler. Fires a {@link HandlingError handling error} event
	 * for this event and the given throwable.
	 * 
	 * @see HandlingError
	 */
	@Override
	protected void handlingError(
			EventPipeline eventProcessor, Throwable throwable) {
		eventProcessor.fire(
				new HandlingError(this, throwable), channels());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (channels != null) {
			builder.append("channels=");
			builder.append(Channel.toString(channels));
		}
		builder.append("]");
		return builder.toString();
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
	 * Returns the channels associated with the event. Before an
	 * event has been fired, this returns the channels set with
	 * {@link #setChannels(Channel[])}. After an event has been
	 * fired, this returns the channels that the event has
	 * effectively been fired on 
	 * (see {@link Manager#fire(Event, Channel...)}).
	 * 
	 * @return the channels
	 */
	@Override
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
	 * and all events caused by it have been handled. Such an
	 * event is called a "completion event".
	 * 
	 * Completion events are considered to be caused by the event that 
	 * caused the completed event. If an event *e1* caused an event
	 * *e2* which has a completion event *e2c*, *e1* is only put in 
	 * state completed when *e2c* has been handled.
	 * 
	 * Completion events are handled by the same {@link EventProcessor}
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
	public synchronized Event<T> tieTo(Event<T> other) {
		if (other.results == null) {
			other.results = new ArrayList<T>();
		}
		results = other.results;
		return (Event<T>)this;
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
		if (with != null) {
			contextData.put(by, with);
		} else {
			contextData.remove(by);
		}
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