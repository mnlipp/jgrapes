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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;

/**
 * This class provides the default implementation of an {@link EventPipeline}.
 * 
 * @author Michael N. Lipp
 */
public class EventProcessor implements MergingEventPipeline, Runnable {

	private static ExecutorService executorService 
		= Executors.newCachedThreadPool();
	
	private static ThreadLocal<EventProcessor> 
		currentPipeline = new ThreadLocal<>();
	private ComponentTree componentTree;
	private EventQueue queue = new EventQueue();
	private EventBase currentlyHandling = null;
	
	EventProcessor (ComponentTree tree) {
		this.componentTree = tree;
	}

	@Override
	public void add(EventBase event, Channel... channels) {
		EventProcessor pipeline = currentPipeline.get();
		if (pipeline == this) {
			// If this pipeline is associated with the current thread, 
			// the event has been fired while processing some previous 
			// (triggering) event from this pipeline. Simply add the 
			// new event at the end of the queue, noting the event currently
			// being processed (if any) as cause. 
			((EventBase)event).generatedBy(currentlyHandling);
			synchronized (queue) {
				queue.add(event, channels);
			}
			return;
		}
		// Event is being added from a thread that is not the thread
		// running this pipeline (if any).
		((EventBase)event).generatedBy(null);
		synchronized (queue) {
			boolean wasEmpty = queue.isEmpty();
			queue.add(event, channels);
			if (wasEmpty) {
				executorService.submit(this);
			}
		}
	}

	@Override
	public void merge(EventPipeline other) {
		if (other instanceof EventBuffer) {
			add(((EventBuffer) other).retrieveEvents());
		}
		throw new IllegalArgumentException
			("Can only merge events from an EventBuffer.");
	}

	void add(EventQueue source) {
		EventProcessor pipeline = currentPipeline.get();
		if (pipeline != null) {
			pipeline.queue.addAll(source);
			source.clear();
		}
		synchronized (queue) {
			boolean wasEmpty = queue.isEmpty();
			queue.addAll(source);
			source.clear();
			if (wasEmpty) {
				executorService.submit(this);
			}
		}
	}
	
	@Override
	synchronized public void run() {
		if (queue.isEmpty()) {
			return;
		}
		try {
			currentPipeline.set(this);
			while (true) {
				EventChannelsTuple next = queue.peek();
				currentlyHandling = next.event;
				componentTree.dispatch(this, next.event, next.channels);
				currentlyHandling.decrementOpen(this);
				synchronized (queue) {
					queue.remove();
					if (queue.isEmpty()) {
						break;
					}
				}
			}
		} finally {
			currentlyHandling = null;
			currentPipeline.set(null);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EventProcessor [");
		if (queue != null) {
			builder.append("queue=");
			builder.append(queue);
			builder.append(", ");
		}
		builder.append("]");
		return builder.toString();
	}
	
	
}
