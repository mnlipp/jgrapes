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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 *
 */
class SynchronousEventProcessor extends EventProcessor {

	private boolean isRunning = false;
	
	public SynchronousEventProcessor(ComponentTree tree) {
		super(tree);
	}

	/* (non-Javadoc)
	 * @see EventProcessor#add(EventBase, org.jgrapes.core.Channel[])
	 */
	@Override
	public <T extends Event<?>> T add(T event, Channel... channels) {
		((EventBase<?>)event).generatedBy(newEventsParent.get());
		((EventBase<?>)event).processedBy(this);
		synchronized(queue) {
			queue.add(event, channels);
		}
		if (isRunning) {
			return event;
		}
		isRunning = true;
		GeneratorRegistry.instance().add(this);
		run();
		isRunning = false;
		return event;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.EventProcessor#run()
	 */
	@Override
	public synchronized void run() {
		// Save current event pipeline and currently handled event
		// because a SynchronousEventPipeline can be called while 
		// handling an event (from another event processor).
		InternalEventPipeline currentPipeline 
			= FeedBackPipelineFilter.getAssociatedPipeline();
		EventBase<?> currentEvent = newEventsParent.get();
		try {
			super.run();
		} finally {
			newEventsParent.set(currentEvent);
			FeedBackPipelineFilter.setAssociatedPipeline(currentPipeline);;
		}
	}
	
}
