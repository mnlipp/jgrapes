/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.core.internal;

import java.time.Instant;
import org.jgrapes.core.Components;

/**
 * The base class for completeion locks.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class CompletionLockBase {

    private final EventBase<?> event;
    private final long timeout;
    private Components.Timer timer;

    /**
     * @param event the event to be locked
     * @param timeout
     */
    protected CompletionLockBase(EventBase<?> event, long timeout) {
        this.event = event;
        this.timeout = timeout;
        event.addCompletionLock(this);
    }

    /* default */ long getTimeout() {
        return timeout;
    }

    /**
     * Removes this completion lock from the event that it was created for.
     * 
     * This method may be invoked even if the completion lock has already
     * been removed. This allows locks to be used for disjunctive wait.
     */
    public void remove() {
        event.removeCompletionLock(this);
    }

    /* default */ CompletionLockBase startTimer() {
        if (timeout == 0) {
            return this;
        }
        timer = Components.schedule(scheduledFor -> {
            event.removeCompletionLock(this);
        }, Instant.now().plusMillis(timeout));
        return this;
    }

    /* default */ void cancelTimer() {
        synchronized (this) {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }
}
