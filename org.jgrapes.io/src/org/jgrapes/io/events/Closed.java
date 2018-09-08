/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io.events;

import java.io.IOException;
import java.util.Optional;

import org.jgrapes.core.Event;

/**
 * This event signals that an I/O subchannel will no longer be used.
 * Components that have allocated resources for that channel should
 * release them in response to receiving this event.
 */
public class Closed extends Event<Void> {

    private Throwable error;

    /**
     * Creates a new event that signals a close in response to
     * an error (usually an {@link IOException}.
     */
    public Closed(Throwable error) {
        this.error = error;
    }

    /**
     * Creates a new event that signals a regular close.
     */
    public Closed() {
        this(null);
    }

    /**
     * Returns the cause of the {@link Closed} event in case of error.
     *
     * @return the optional failure
     */
    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }
}
