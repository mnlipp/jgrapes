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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jgrapes.core.Channel;
import org.jgrapes.core.AttachedComponent;
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
 * 
 * @author Michael N. Lipp
 */
public abstract class EventBase<T> implements Matchable, Future<T> {

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
	private Set<Event<?>> completedEvents = null;
	/** Set when the event has been completed. */
	private boolean completed = false;
	/** Indicates that the event should not processed further. */
	private boolean stopped = false;
	/** The result of handling the event (if any). */
	private AtomicReference<T> result;
	
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
	 * Look through the event'channels and return the first
	 * (and usually only) channel of given type.
	 * 
	 * @param <C> the given type's class
	 * @param type the class to look for
	 * @return the channel or {@code null}
	 */
	public <C> C firstChannel(Class<C> type) {
		for (Channel channel: channels) {
			if (type.isAssignableFrom(channel.getClass())) {
				return type.cast(channel);
			}
		}
		return null;
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
			throw new IllegalStateException
				("Channels cannot be changed after fire");
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
	 * Sets the result of handling this event.
	 * 
	 * @param result the result to set
	 * @return the object for easy chaining
	 */
	public Event<T> setResult(T result) {
		if (this.result == null) {
			this.result = new AtomicReference<T>(result);
			return (Event<T>)this;
		}
		this.result.set(result);
		return (Event<T>)this;
	}

	/**
	 * Allows access to the intermediate result before the 
	 * completion of the event. 
	 * 
	 * @return the intermediate result
	 */
	protected T getResult() {
		return result == null ? null : result.get();
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
	public Event<T> tieTo(EventBase<T> other) {
		if (other.result == null) {
			other.result = new AtomicReference<T>(null);
		}
		result = other.result;
		return (Event<T>)this;
	}
	
	/**
	 * Invoked when an exception occurs while invoking a handler for an event.
	 * 
	 * @param eventProcessor the manager that has invoked the handler
	 * @param throwable the exception that has been thrown by the handler
	 */
	protected abstract void handlingError
		(EventPipeline eventProcessor, Throwable throwable);

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
	 * Returns <code>true</code> if {@link stop} has been called.
	 * 
	 * @return the stopped state
	 */
	boolean isStopped() {
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
	 * @param pipeline
	 */
	void decrementOpen(InternalEventPipeline pipeline) {
		if (openCount.decrementAndGet() == 0 && !completed) {
			synchronized (this) {
				completed = true;
				notifyAll();
			}
			if (completedEvents != null) {
				for (Event<?> e: completedEvents) {
					Channel[] completeChannels = e.channels();
					if (completeChannels == null) {
						// Note that channels cannot be null, as it is set
						// when firing the event and an event is never fired
						// on no channels.
						completeChannels = channels;
						e.setChannels(completeChannels);
					}
					pipeline.add(e, completeChannels);
				}
			}
			if (generatedBy != null) {
				generatedBy.decrementOpen(pipeline);
			}
		}
	}

	/**
	 * Returns the events to be thrown when this event and all events caused
	 * by it have been handled.
	 * 
	 * @return the completed events
	 */
	@SuppressWarnings("unchecked")
	public Set<Event<?>> getCompletedEvents() {
		return completedEvents == null ? (Set<Event<?>>)Collections.EMPTY_SET
				: Collections.unmodifiableSet(completedEvents);
	}

	/**
	 * Adds the event to the events to be thrown when this event and all 
	 * events caused by it have been handled.
	 * 
	 * @param completedEvent the completedEvent to add
	 * @return the object for easy chaining
	 */
	public Event<T> addCompletedEvent(Event<?> completedEvent) {
		if (completedEvents == null) {
			completedEvents = new HashSet<>();
		}
		completedEvents.add(completedEvent);
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
	 * The cancel semantics of {@link Future}s do not apply to events.
	 * 
	 * @param mayInterruptIfRunning ignored
	 * @return always {@code false} as event processing cannot be cancelled
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	/**
	 * The cancel semantics of {@link Future}s do not apply to events.
	 * 
	 * @return always {@code false} as event processing cannot be cancelled
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public T get() throws InterruptedException {
		while (true) {
			synchronized(this) {
				if (completed) {
					return result == null ? null : result.get();
				}
				wait();
			}
		}
	}

	/**
	 * Causes the invoking thread to wait until the processing of the 
	 * event has been completed or given timeout has expired. 
	 * 
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public T get(long timeout, TimeUnit unit)
	        throws InterruptedException, TimeoutException {
		synchronized(this) {
			if (completed) {
				return result == null ? null : result.get();
			}
			wait(unit.toMillis(timeout));
		}
		if (completed) {
			return result == null ? null : result.get();
		}
		throw new TimeoutException();
	}

	/**
	 * Sets the data that is stored in the executing pipeline for
	 * the given component.
	 * 
	 * @param component the component
	 * @param data the data
	 * @return the object for easy chaining
	 */
	public Event<T> setComponentContext(AttachedComponent component, Object data) {
		FeedBackPipelineFilter.setComponentContext(component, data);
		return (Event<T>)this;
	}
	
	/**
	 * Gets the data that is stored in the executing pipeline for
	 * the given component.
	 * 
	 * @param component the component
	 * @return the data
	 */
	public Object getComponentContext(AttachedComponent component) {
		return FeedBackPipelineFilter.getComponentContext(component);
	}
}
