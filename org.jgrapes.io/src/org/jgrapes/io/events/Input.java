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

import java.nio.Buffer;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * This event signals that a new chunk of data has successfully been obtained
 * from some source. This type of event is commonly
 * used for data flowing into the application.
 * 
 * @param <T> the type of data used in this event
 */
public final class Input<T extends Buffer> extends IOEvent<T> {

    private Input(ManagedBuffer<T> buffer, boolean endOfRecord) {
        super(buffer, endOfRecord);
    }

    /**
     * Create a new event with the given buffer. The buffer must
     * have been prepared for invoking `get`-methods.
     * 
     * @param buffer the buffer with the data
     * @param endOfRecord if the event ends a data record
     */
    public static <B extends Buffer> Input<B> fromSource(
            ManagedBuffer<B> buffer, boolean endOfRecord) {
        return new Input<>(buffer, endOfRecord);
    }

    /**
     * Create a new event with the given buffer. Creating the event
     * flips the buffer, which is assumed to have been used for
     * collecting data up to now.
     * 
     * @param buffer the buffer with the data
     * @param endOfRecord if the event ends a data record
     */
    public static <B extends Buffer> Input<B> fromSink(
            ManagedBuffer<B> buffer, boolean endOfRecord) {
        buffer.flip();
        return new Input<>(buffer, endOfRecord);
    }
}
