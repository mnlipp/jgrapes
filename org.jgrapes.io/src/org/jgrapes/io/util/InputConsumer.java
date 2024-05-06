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

import java.nio.Buffer;
import org.jgrapes.io.events.Input;

/**
 * May be implemented by classes that can consume input events to support
 * generic usage.
 * 
 * @since 2.8.0
 */
public interface InputConsumer {

    /**
     * Feed data to the consumer. The call blocks while data from a previous
     * invocation has not been fully read. The buffer passed as argument
     * is locked (see {@link ManagedBuffer#lockBuffer()}) until all
     * data has been consumed.
     * 
     * Calling this method with `null` indicates the end of the feed.
     *
     * @param buffer the buffer
     */
    <W extends Buffer> void feed(ManagedBuffer<W> buffer);

    /**
     * Calls {@link #feed(ManagedBuffer)} with the provided event's
     * buffer.
     * 
     * Calling this method with `null` indicates the end of the feed.
     *
     * @param event the event
     */
    default <W extends Buffer> void feed(Input<W> event) {
        if (event == null) {
            feed((ManagedBuffer<W>) null);
        } else {
            feed(event.buffer());
        }
    }

}
