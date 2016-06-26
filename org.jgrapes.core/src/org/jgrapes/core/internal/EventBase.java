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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;

/**
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
	private int openCount = 0;
	/** The event to be fired upon completion. */
	private Event<?> completedEvent = null;
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
	public Channel[] getChannels() {
		return channels;
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
	public EventBase<T> setChannels(Channel... channels) {
		if (enqueued()) {
			throw new IllegalStateException
				("Channels cannot be changed after fire");
		}
		this.channels = channels;
		return this;
	}

	/**
	 * Returns <code>true</code> if the event has been enqueued in a pipeline.
	 * 
	 * @return the result
	 */
	protected boolean enqueued() {
		return openCount > 0;
	}

	/**
	 * Sets the result of handling this event.
	 * 
	 * @param result
	 * @return the object for easy chaining
	 */
	public EventBase<T> setResult(T result) {
		if (this.result == null) {
			this.result = new AtomicReference<T>(result);
			return this;
		}
		this.result.set(result);
		return this;
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
	 * @param other
	 * @return the object for easy chaining
	 */
	public EventBase<T> tieTo(EventBase<T> other) {
		if (other.result == null) {
			other.result = new AtomicReference<T>(null);
		}
		result = other.result;
		return this;
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
	public EventBase<T> stop() {
		stopped = true;
		return this;
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
		openCount += 1;
		this.generatedBy = causingEvent;
		if (causingEvent != null) {
			causingEvent.openCount += 1;
		}
	}

	/**
	 * @param pipeline
	 */
	void decrementOpen(EventPipeline pipeline) {
		openCount -= 1;
		if (openCount == 0 && !completed) {
			synchronized (this) {
				completed = true;
				notifyAll();
			}
			if (completedEvent != null) {
				Channel[] completeChannels = completedEvent.getChannels();
				if (completeChannels == null) {
					completeChannels = channels;
				}
				pipeline.add(completedEvent, completeChannels);
			}
			if (generatedBy != null) {
				generatedBy.decrementOpen(pipeline);
			}
		}
	}

	/**
	 * Returns the event to be thrown when this event and all events caused
	 * by it have been handled.
	 * 
	 * @return the completedEvent
	 */
	public Event<?> getCompletedEvent() {
		return completedEvent;
	}

	/**
	 * Sets the event to be thrown when this event and all events caused
	 * by it have been handled.
	 * 
	 * @param completedEvent the completedEvent to set
	 * @return the object for easy chaining
	 */
	public EventBase<T> setCompletedEvent(Event<?> completedEvent) {
		this.completedEvent = completedEvent;
		return this;
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
	public EventBase<T> setComponentContext(Component component, Object data) {
		FeedBackPipelineFilter.setComponentContext(component, data);
		return this;
	}
	
	/**
	 * Gets the data that is stored in the executing pipeline for
	 * the given component.
	 * 
	 * @param component the component
	 * @return the data
	 */
	public Object getComponentContext(Component component) {
		return FeedBackPipelineFilter.getComponentContext(component);
	}
}
