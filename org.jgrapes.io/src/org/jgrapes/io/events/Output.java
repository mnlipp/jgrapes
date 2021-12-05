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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * This event signals that a new chunk of internally generated data is to be
 * forwarded to some destination. This type of
 * event is commonly used for data flowing out of the application.
 */
public class Output<T extends Buffer> extends IOEvent<T> {

    /**
     * Create a new output event with the given buffer and optionally flips
     * it. Used internally for constructor ("super(...)") invocations that 
     * don't flip the buffer.
     * 
     * @param buffer the buffer with the data
     * @param flip if the buffer should be flipped
     * @param endOfRecord if the event ends a data record
     */
    private Output(ManagedBuffer<T> buffer, boolean flip, boolean endOfRecord) {
        super(buffer, endOfRecord);
        if (flip) {
            buffer.flip();
        }
    }

    /**
     * Create a new event from an existing event. This constructor
     * is useful if the data is to be forwarded to another channel
     * by a new event.
     * 
     * The buffer is reused in the new event (the lock count is 
     * incremented).
     * 
     * @param event the existing event
     */
    public Output(Output<T> event) {
        this(event.buffer(), false, event.isEndOfRecord());
        event.buffer().lockBuffer();
    }

    /**
     * Create a new event with the given buffer. The buffer must
     * have been prepared for invoking `get`-methods.
     * 
     * @param buffer the buffer with the data
     * @param endOfRecord if the event ends a data record
     */
    public static <B extends Buffer> Output<B> fromSource(
            ManagedBuffer<B> buffer, boolean endOfRecord) {
        return new Output<>(buffer, false, endOfRecord);
    }

    /**
     * Create a new event with the given buffer. Creating the event
     * flips the buffer, which is assumed to have been used for
     * collecting data up to now.
     * 
     * @param buffer the buffer with the data
     * @param endOfRecord if the event ends a data record
     */
    public static <B extends Buffer> Output<B> fromSink(
            ManagedBuffer<B> buffer, boolean endOfRecord) {
        return new Output<>(buffer, true, endOfRecord);
    }

    /**
     * Convenience method that creates a 
     * {@code Output<CharBuffer>} event from a {@link String}.
     * 
     * @param data the string to wrap
     * @param endOfRecord if the event ends a data record
     * @return the event
     */
    public static Output<CharBuffer>
            from(String data, boolean endOfRecord) {
        return new Output<>(ManagedBuffer.wrap(
            CharBuffer.wrap(data)), false, endOfRecord);
    }

    /**
     * Convenience method that creates a 
     * {@code Output<ByteBuffer>} event from a `byte[]`.
     * 
     * @param data the array to wrap
     * @param endOfRecord if the event ends a data record
     * @return the event
     */
    public static Output<ByteBuffer>
            from(byte[] data, boolean endOfRecord) {
        return new Output<>(ManagedBuffer.wrap(ByteBuffer.wrap(data)),
            false, endOfRecord);
    }
}
