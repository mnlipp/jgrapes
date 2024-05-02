/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.io.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Cleans up threads if some object has been garbage collected.
 * 
 * Sometimes it is necessary to create a thread that delivers data
 * to a synchronous consumer. This thread keeps the consumer running
 * until the consumer expects no more input and thus terminates
 * the thread.
 * 
 * Due to an error condition it may happen, however, that the terminating
 * event never occurs and the thread runs forever. As a possible remedy,
 * this class allows the thread to be associated with the lifetime of an
 * arbitrary object. When the object is garbage collected, the thread is
 * terminated automatically.
 * 
 * @since 2.8.0
 */
public final class ThreadCleaner {

    private static Set<RefWithThread> watched = new HashSet<>();
    private static ReferenceQueue<Object> abandoned
        = new ReferenceQueue<>();

    private ThreadCleaner() {
        // Utility class
    }

    /**
     * Weak references to an object that interrupts the associated
     * thread if the object has been garbage collected.
     *
     * @param <T> the generic type
     */
    private static class RefWithThread extends WeakReference<Object> {
        public final Thread watched;

        /**
         * Creates a new instance.
         *
         * @param referent the referent
         * @param thread the thread
         */
        public RefWithThread(Object referent, Thread thread) {
            super(referent, abandoned);
            watched = thread;
        }
    }

    static {
        Thread watchdog = new Thread(() -> {
            Thread.currentThread().setName("ThreadCleaner");
            while (true) {
                try {
                    ThreadCleaner.RefWithThread ref
                        = (ThreadCleaner.RefWithThread) abandoned.remove();
                    ref.watched.interrupt();
                    watched.remove(ref);
                } catch (InterruptedException e) { // NOPMD
                    // Nothing to do
                }
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();
    }

    /**
     * Watch the referent and terminate the thread if it is
     * garbage collected.
     *
     * @param referent the referent
     * @param thread the thread
     */
    public static void watch(Object referent, Thread thread) {
        watched.add(new RefWithThread(referent, thread));
    }
}
