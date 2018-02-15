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

package org.jgrapes.http.freemarker.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;

/**
 * A helper component that can be used to wait for the occurrence of 
 * an event.
 * 
 */
public class WaitForTests extends Component implements Future<Event<?>> {

	Event<?> result = null;
	
	/**
	 * Create a component that attaches itself to the given tree and
	 * waits for the occurrence of an event of the given type on
	 * the given channel. 
	 */
	public WaitForTests(ComponentType app, Object eventKey, Object channelKey) {
		Handler.Evaluator.add(
				this, "onEvent", eventKey, channelKey, Integer.MIN_VALUE);
		Components.manager(app).attach(this);
	}

	/**
	 * Called when the event occurs.
	 * 
	 * @param event
	 */
	@Handler(dynamic=true)
	public void onEvent(Event<?> event) {
		synchronized (this) {
			this.result = event;
			notifyAll();
		}
		detach();
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public Event<?> get() throws InterruptedException, ExecutionException {
		synchronized (this) {
			while (result == null) {
				wait();
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Event<?> get(long timeout, TimeUnit unit)
	        throws InterruptedException, ExecutionException, TimeoutException {
		synchronized (this) {
			while (result == null) {
				wait(unit.toMillis(timeout));
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone() {
		return result != null;
	}
}
