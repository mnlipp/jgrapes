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

/**
 * @author Michael N. Lipp
 *
 */
public class GeneratorRegistry {

	private static final class InstanceHolder {
		static final GeneratorRegistry INSTANCE = new GeneratorRegistry();
	}
	
	private long running = 0;
	private Thread keepAlive;
	
	private GeneratorRegistry() {
	}

	public static GeneratorRegistry getInstance() {
		return InstanceHolder.INSTANCE;
	}
	
	synchronized public void add(Object obj) {
		running += 1;
		if (running == 1) {
			keepAlive = new Thread("GeneratorRegistry") {
				@Override
				public void run() {
					try {
						while (true) {
							Thread.sleep(Long.MAX_VALUE);
						}
					} catch (InterruptedException e) {
					}
				}
			};
			keepAlive.start();
		}
	}
	
	synchronized public void remove(Object obj) {
		running -= 1;
		if (running == 0) {
			keepAlive.interrupt();
			notifyAll();
		}
	}
	
	public boolean isExhausted() {
		return running == 0;
	}
	
	synchronized public void awaitExhaustion() throws InterruptedException {
		while (running > 0) {
			wait();
		}
	}
	
	synchronized public boolean awaitExhaustion(long timeout) 
			throws InterruptedException {
		if (isExhausted()) {
			return true;
		}
		wait(timeout);
		return isExhausted();
	}
}
