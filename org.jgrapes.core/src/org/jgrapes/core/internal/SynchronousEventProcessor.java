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
	public void add(EventBase<?> event, Channel... channels) {
		event.generatedBy(currentlyHandling);
		queue.add(event, channels);
		if (isRunning) {
			return;
		}
		isRunning = true;
		run();
		isRunning = false;			
	}

	
}
