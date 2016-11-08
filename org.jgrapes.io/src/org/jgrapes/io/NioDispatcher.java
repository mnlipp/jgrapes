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
package org.jgrapes.io;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.NioRegistration;

/**
 * A helper component that provides the central hub for non blocking
 * I/O components. Exactly one {@code NioDispatcher} must exist in
 * any tree with {@link NioHandler} components. 
 * 
 * @author Michael N. Lipp
 */
public class NioDispatcher extends Component implements Runnable {

	private Selector selector = null;
	private Thread runner = null;
	private boolean running = false;
	private Object selectorGate = new Object();
	
	/**
	 * Creates a new Dispatcher.
	 * 
	 * @throws IOException if an I/O exception occurred
	 */
	public NioDispatcher() throws IOException {
		selector = Selector.open();
	}

	/**
	 * Starts this dispatcher. A dispatcher has an associated thread that
	 * keeps it running.
	 * 
	 * @param event the event
	 */
	@Handler
	synchronized public void onStart(Start event) {
		if (running) {
			return;
		}
		running = true;
		runner = new Thread(this, "NioDispatcher");
		runner.start();
	}

	/**
	 * Stops the thread that is associated with this dispatcher.
	 * 
	 * @param event the event
	 * @throws InterruptedException if the execution is interrupted
	 */
	@Handler(priority=-10000)
	synchronized public void onStop(Stop event) throws InterruptedException {
		if (runner == null) {
			return;
		}
		running = false;
		// It just might happen that the wakeup() occurs between the
		// check for running and the select() in the thread's run loop,
		// but we -- obviously -- cannot put the select() in a 
		// synchronized(this).
		while (runner.isAlive()) {
			selector.wakeup();
			runner.join(10);
		}
		runner = null;
	}

	/**
	 * Invoked once by the thread associated with the dispatcher. Handles
	 * all events from the underlying {@link Selector}.  
	 */
	@Override
	public void run() {
		try {
			registerAsGenerator();
			while (running) {
				try {
					selector.select();
					Set<SelectionKey> selected = selector.selectedKeys();
					for (SelectionKey key: selected) {
						((NioHandler)key.attachment())
							.handleOps(key.readyOps());
					}
					selected.clear();
					synchronized (selectorGate) {
						// Delay next iteration if another thread has the lock.
						// "Find bugs" complains, but this is really okay.
					}
				} catch (InterruptedIOException e) {
					break;
				} catch (IOException e) {
				}
			}
		} finally {
			unregisterAsGenerator();
		}
	}

	@Handler
	public void onNioRegistration(NioRegistration event)
			throws IOException {
		SelectableChannel channel = event.getIoChannel();
		channel.configureBlocking(false);
		SelectionKey key;
		synchronized (selectorGate) {
			selector.wakeup(); // make sure selector isn't blocking
			key = channel.register
					(selector, event.getOps(), event.getHandler());
		}
		event.setResult(new Registration(key));
	}
	
	public class Registration extends NioRegistration.Registration {

		private SelectionKey key;
		
		public Registration(SelectionKey key) {
			super();
			this.key = key;
		}

		@Override
		public void updateInterested(int ops) {
			synchronized (selectorGate) {
				selector.wakeup(); // make sure selector isn't blocking
				key.interestOps(ops);
			}
		}
	}
	
}
