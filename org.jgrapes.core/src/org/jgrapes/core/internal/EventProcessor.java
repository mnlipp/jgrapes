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

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * This class provides the default implementation of an {@link EventPipeline}.
 */
public class EventProcessor implements ExecutingEventPipeline, Runnable {

	private static ExecutorService defaultExecutorService 
		= Executors.newCachedThreadPool();
	
	protected static ThreadLocal<EventBase<?>> 
		currentlyHandling = new ThreadLocal<>();
	
	private ExecutorService executorService;
	private ComponentTree componentTree;
	private EventPipeline asEventPipeline;
	protected EventQueue queue = new EventQueue();
	private WeakHashMap<ComponentType, Object> componentContext 
		= new WeakHashMap<>();
	
	EventProcessor(ComponentTree tree) {
		this(tree, defaultExecutorService);
	}

	EventProcessor(ComponentTree tree, ExecutorService executorService) {
		this.componentTree = tree;
		this.executorService = executorService;
		asEventPipeline = new CheckingPipelineFilter(this);
	}

	@Override
	public <T extends Event<?>> T add(T event, Channel... channels) {
		((EventBase<?>)event).generatedBy(currentlyHandling.get());
		synchronized (queue) {
			boolean wasEmpty = queue.isEmpty();
			queue.add(event, channels);
			if (wasEmpty) {
				GeneratorRegistry.instance().add(this);
				executorService.submit(this);
			}
		}
		return event;
	}

	void add(EventQueue source) {
		synchronized (queue) {
			boolean wasEmpty = queue.isEmpty();
			queue.addAll(source);
			source.clear();
			if (wasEmpty) {
				GeneratorRegistry.instance().add(this);
				executorService.submit(this);
			}
		}
	}
	
	@Override
	public void merge(InternalEventPipeline other) {
		if (!(other instanceof EventBuffer)) {
			throw new IllegalArgumentException(
					"Can only merge events from an EventBuffer.");
		}
		add(((EventBuffer) other).retrieveEvents());
	}

	@Override
	public synchronized void run() {
		try {
			if (queue.isEmpty()) {
				return;
			}
			FeedBackPipelineFilter.setAssociatedPipeline(this);
			while (true) {
				EventChannelsTuple next = queue.peek();
				currentlyHandling.set(next.event);
				componentTree.dispatch(
						asEventPipeline, next.event, next.channels);
				currentlyHandling.get().decrementOpen(this);
				synchronized (queue) {
					queue.remove();
					if (queue.isEmpty()) {
						break;
					}
				}
			}
		} finally {
			GeneratorRegistry.instance().remove(this);
			currentlyHandling.set(null);;
			FeedBackPipelineFilter.setAssociatedPipeline(null);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (queue != null) {
			builder.append("queue=");
			builder.append(queue);
		}
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.EventPipeline#setContext(org.jgrapes.core.Component, java.lang.Object)
	 */
	@Override
	public void setComponentContext(ComponentType component, Object data) {
		componentContext.put(component, data);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.EventPipeline#getContext(org.jgrapes.core.Component)
	 */
	@Override
	public Object componentContext(ComponentType component) {
		return componentContext.get(component);
	}
}
