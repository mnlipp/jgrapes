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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.ComponentType;

/**
 * A registry for generators. Used to track generators and determine
 * whether the application has stopped.
 */
public class GeneratorRegistry {

    @SuppressWarnings("PMD.VariableNamingConventions")
    private static final Logger generatorTracking
        = Logger.getLogger(ComponentType.class.getPackage().getName()
            + ".generatorTracking");

    private long running;
    private Thread keepAlive;
    private Map<Object, Object> generators;

    /**
     * Holds a generator instance.
     */
    private static final class InstanceHolder {
        @SuppressWarnings("PMD.AccessorClassGeneration")
        private static final GeneratorRegistry INSTANCE
            = new GeneratorRegistry();
    }

    private GeneratorRegistry() {
        if (generatorTracking.isLoggable(Level.FINE)) {
            generators = new IdentityHashMap<>();
        }
    }

    /**
     * Returns the singleton instance of the registry.
     *
     * @return the generator registry
     */
    public static GeneratorRegistry instance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Adds a generator.
     *
     * @param obj the obj
     */
    @SuppressWarnings({ "PMD.GuardLogStatement", "PMD.AvoidDuplicateLiterals" })
    public void add(Object obj) {
        synchronized (this) {
            running += 1;
            if (generators != null) {
                generators.put(obj, null);
                generatorTracking.finest(() -> "Added generator " + obj
                    + ", " + generators.size() + " generators registered: "
                    + generators.keySet());
            }
            if (running == 1) { // NOPMD, no, not using a constant for this.
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
    }

    /**
     * Removes the generator.
     *
     * @param obj the generator
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    public void remove(Object obj) {
        synchronized (this) {
            running -= 1;
            if (generators != null) {
                generators.remove(obj);
                generatorTracking.finest(() -> "Removed generator " + obj
                    + ", " + generators.size() + " generators registered: "
                    + generators.keySet());
            }
            if (running == 0) {
                keepAlive.interrupt();
                notifyAll();
            }
        }
    }

    /**
     * Checks if is exhausted (no generators left)
     *
     * @return true, if is exhausted
     */
    public boolean isExhausted() {
        return running == 0;
    }

    /**
     * Await exhaustion.
     *
     * @throws InterruptedException the interrupted exception
     */
    @SuppressWarnings({ "PMD.CollapsibleIfStatements",
        "PMD.GuardLogStatement" })
    public void awaitExhaustion() throws InterruptedException {
        synchronized (this) {
            if (generators != null) {
                if (running != generators.size()) {
                    generatorTracking
                        .severe(() -> "Generator count doesn't match tracked.");
                }
            }
            while (running > 0) {
                if (generators != null) {
                    generatorTracking
                        .fine(() -> "Thread " + Thread.currentThread().getName()
                            + " is waiting, " + generators.size()
                            + " generators registered: "
                            + generators.keySet());
                }
                wait();
            }
            generatorTracking
                .finest("Thread " + Thread.currentThread().getName()
                    + " continues.");
        }
    }

    /**
     * Await exhaustion with a timeout.
     *
     * @param timeout the timeout
     * @return true, if successful
     * @throws InterruptedException the interrupted exception
     */
    @SuppressWarnings({ "PMD.CollapsibleIfStatements",
        "PMD.GuardLogStatement" })
    public boolean awaitExhaustion(long timeout)
            throws InterruptedException {
        synchronized (this) {
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
                generatorTracking
                    .fine(() -> "Waiting, generators: " + generators.keySet());
            }
            wait(timeout);
            if (generators != null) {
                generatorTracking
                    .fine(() -> "Waited, generators: " + generators.keySet());
            }
            return isExhausted();
        }
    }
}
