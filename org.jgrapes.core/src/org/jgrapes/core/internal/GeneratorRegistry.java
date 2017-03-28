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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.ComponentType;

/**
 * @author Michael N. Lipp
 *
 */
public class GeneratorRegistry {

	private static final Logger generatorTracking 
		= Logger.getLogger(ComponentType.class.getPackage().getName() 
		+ ".generatorTracking");

	private static final class InstanceHolder {
		static final GeneratorRegistry INSTANCE = new GeneratorRegistry();
	}
	
	private long running = 0;
	private Thread keepAlive;
	private Map<Object,Object> generators = null;
	
	private GeneratorRegistry() {
		if (generatorTracking.isLoggable(Level.FINE)) {
			generators = new IdentityHashMap<>();
		}
	}

	public static GeneratorRegistry instance() {
		return InstanceHolder.INSTANCE;
	}
	
	public synchronized void add(Object obj) {
		running += 1;
		if (generators != null) {
			generators.put(obj, null);
		}
		if (running == 1) {
			keepAlive = new Thread("GeneratorRegistry") {
				@Override
				public void run() {
					try {
						while (true) {
							Thread.sleep(Long.MAX_VALUE);
						}
					} catch (InterruptedException e) {
						// Okay, then stop
					}
				}
			};
			keepAlive.start();
		}
	}
	
	public synchronized void remove(Object obj) {
		running -= 1;
		if (generators != null) {
			generators.remove(obj);
		}
		if (running == 0) {
			keepAlive.interrupt();
			notifyAll();
		}
	}
	
	public boolean isExhausted() {
		return running == 0;
	}
	
	public synchronized void awaitExhaustion() throws InterruptedException {
		if (generators != null) {
			if (running != generators.size()) {
				generatorTracking.severe(
						"Generator count doesn't match tracked.");
			}
		}
		while (running > 0) {
			if (generators != null) {
				generatorTracking.fine(
						"Waiting, generators: " + generators.keySet());
			}
			wait();
		}
	}
	
	public synchronized boolean awaitExhaustion(long timeout) 
			throws InterruptedException {
		if (generators != null) {
			if (running != generators.size()) {
				generatorTracking.severe(
						"Generator count doesn't match tracked.");
			}
		}
		if (isExhausted()) {
			return true;
		}
		if (generators != null) {
			generatorTracking.fine(
					"Waiting, generators: " + generators.keySet());
		}
		wait(timeout);
		if (generators != null) {
			generatorTracking.fine(
					"Waited, generators: " + generators.keySet());
		}
		return isExhausted();
	}
}
