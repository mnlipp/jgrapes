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

/**
 * @author Michael N. Lipp
 *
 */
class SynchronousEventProcessor extends EventProcessor {

	private boolean isRunning = false;
	
	public SynchronousEventProcessor(ComponentTree tree) {
		super(tree);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.EventProcessor#add(org.jgrapes.core.internal.EventBase, org.jgrapes.core.Channel[])
	 */
	@Override
	public <T> Event<T> add(Event<T> event, Channel... channels) {
		((EventBase<?>)event).generatedBy(currentlyHandling.get());
		queue.add(event, channels);
		if (isRunning) {
			return event;
		}
		isRunning = true;
		GeneratorRegistry.getInstance().add(this);
		run();
		isRunning = false;
		return event;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.EventProcessor#run()
	 */
	@Override
	synchronized public void run() {
		// Save current event pipeline because a SynchronousEventPipeline
		// can be called while handling an event (from another event 
		// processor).
		ExecutingEventPipeline currentPipeline 
			= FeedBackPipelineFilter.getAssociatedPipeline();
		try {
			super.run();
		} finally {
			FeedBackPipelineFilter.setAssociatedPipeline(currentPipeline);;
		}
	}
	
}
