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
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.events.Start;

/**
 * The event buffer is used before a tree has been started. It simply
 * buffers all events until a {@link Start} event is added.
 * 
 * @author Michael N. Lipp
 */
public class EventBuffer implements MergingEventPipeline {
	
	private ComponentTree componentTree;
	/** Buffered events. */
	private EventQueue buffered = new EventQueue();
	/** The event pipeline that we delegate to after the start
	 * event has been detected. */
	private EventProcessor processor = null;
	
	EventBuffer(ComponentTree componentTree) {
		super();
		this.componentTree = componentTree;
	}

	@Override
	synchronized public void merge(EventPipeline other) {
		if (other instanceof EventBuffer) {
			buffered.addAll(((EventBuffer) other).retrieveEvents());
		}
		throw new IllegalArgumentException
			("Can only merge events from an EventBuffer.");
	}

	@Override
	synchronized public void add(EventBase event, Channel... channels) {
		// If thread1 adds the start event and thread2 gets here before we
		// have changed the event processor for the tree, forward the
		// event to the event processor that should already have been used.
		if (processor != null) {
			processor.add(event, channels);
			return;
		}
		// Event gets enqueued (increments reference count).
		((EventBase)event).generatedBy(null);
		buffered.add(event, channels);
		if (event instanceof Start) {
			processor = new EventProcessor(componentTree);
			processor.add(buffered);
			componentTree.setEventPipeline(processor);
		}
	}

	synchronized EventQueue retrieveEvents() {
		EventQueue old = buffered;
		buffered = new EventQueue();
		return old;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EventBuffer [");
		if (buffered != null) {
			builder.append("buffered=");
			builder.append(buffered);
		}
		builder.append("]");
		return builder.toString();
	}
}
