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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;

/**
 * @author mnl
 *
 */
public abstract class EventBase implements Matchable {

	/** The channels that this event is to be fired on if no
	 * channels are specified explicitly when firing. */
	protected Channel[] channels = null;
	/** The event that caused this event. */
	private EventBase generatedBy = null;
	/** Number of events that have to be dispatched until completion.
	 * This is one for the event itself and one more for each event
	 * that has this event as its cause. */
	private int openCount = 0;
	/** The event to be fired upon completion. */
	private Event completedEvent = null;
	/** Set when the event has been completed. */
	private boolean completed = false;
	
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
	 * 
	 * @throws IllegalStateException if the method is called after
	 * this event has been fired
	 */
	public void setChannels(Channel[] channels) {
		if (enqueued()) {
			throw new IllegalStateException
				("Channels cannot be changed after fire");
		}
		this.channels = channels;
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
	 * Invoked when an exception occurs while invoking a handler for an event.
	 * 
	 * @param eventProcessor the manager that has invoked the handler
	 * @param throwable the exception that has been thrown by the handler
	 */
	protected abstract void handlingError
		(EventPipeline eventProcessor, Throwable throwable);
	
	/**
	 * If an event is fired while processing another event, note
	 * the event being processed. This allows us to track the cause
	 * of events to the "initial" (externally) generated event that
	 * triggered everything.
	 * 
	 * @param causingEvent the causing event to set
	 */
	void generatedBy(EventBase causingEvent) {
		openCount += 1;
		this.generatedBy = causingEvent;
		if (causingEvent != null) {
			causingEvent.openCount += 1;
		}
	}

	/**
	 * @param pipeline
	 */
	public void decrementOpen(EventPipeline pipeline) {
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
	public Event getCompletedEvent() {
		return completedEvent;
	}

	/**
	 * Sets the event to be thrown when this event and all events caused
	 * by it have been handled.
	 * 
	 * @param completedEvent the completedEvent to set
	 */
	public void setCompletedEvent(Event completedEvent) {
		this.completedEvent = completedEvent;
	}

	/**
	 * Check if this event has been completed.
	 * 
	 * @return the completed state
	 */
	public boolean isCompleted() {
		return completed;
	}

	/**
	 * Similar to calling <code>awaitComplted(0)</code>.
	 * 
	 * @throws IllegalStateException
	 * @throws InterruptedException
	 */
	public void awaitCompleted() 
			throws IllegalStateException, InterruptedException {
		awaitCompleted(0);
	}

	/**
	 * Causes the invoking thread to wait until the processing of the 
	 * event has been completed or given timeout has expired. 
	 *  
	 * @param timeout the maximum time to wait; if 0, wait forever
	 * @throws InterruptedException if the calling thread is interrupted
	 */
	public void awaitCompleted(long timeout) 
			throws IllegalStateException, InterruptedException {
		while (true) {
			synchronized(this) {
				if (completed) {
					return;
				}
				wait();
			}
		}
	}
}
