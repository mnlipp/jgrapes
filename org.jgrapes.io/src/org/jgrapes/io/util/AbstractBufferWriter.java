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

package org.jgrapes.io.util;

import java.io.Writer;
import java.nio.Buffer;
import java.util.Map;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;

/**
 * A base class for {@link CharBufferWriter} and {@link ByteBufferWriter}.
 */
public abstract class AbstractBufferWriter<B extends Buffer> extends Writer {

    protected IOSubchannel channel;
    private EventPipeline eventPipeline;
    private boolean sendInputEvents;
    protected ManagedBuffer<B> buffer;
    private boolean sendClose = true;
    private boolean sendEor = true;
    private boolean eorSent;
    private boolean isClosed;
    private Map<Object, Object> eventAssociations;

    /**
     * Creates a new instance that uses {@link Output} events to dispatch
     * buffers on the given channel, using the given event pipeline.
     * 
     * @param channel
     *            the channel to fire events on
     * @param eventPipeline
     *            the event pipeline used for firing events
     */
    public AbstractBufferWriter(IOSubchannel channel,
            EventPipeline eventPipeline) {
        this.channel = channel;
        this.eventPipeline = eventPipeline;
    }

    /**
     * Creates a new instance that uses {@link Output} events to dispatch
     * buffers on the given channel, using the channel's response pipeline.
     * 
     * @param channel the channel to fire events on
     */
    public AbstractBufferWriter(IOSubchannel channel) {
        this(channel, channel.responsePipeline());
    }

    /**
     * Causes the data to be fired as {@link Input} events rather
     * than the usual {@link Output} events. 
     * 
     * @return this object for easy chaining
     */
    protected AbstractBufferWriter<B> sendInputEvents() {
        sendInputEvents = true;
        return this;
    }

    /**
     * Suppresses sending of a close event when the stream is closed. 
     * 
     * @return this object for easy chaining
     */
    public AbstractBufferWriter<B> suppressClose() {
        sendClose = false;
        return this;
    }

    /**
     * Suppresses setting the end of record flag when the stream is 
     * flushed or closed.
     * 
     * @return this object for easy chaining
     * @see Output#isEndOfRecord()
     */
    public AbstractBufferWriter<B> suppressEndOfRecord() {
        sendEor = false;
        return this;
    }

    /**
     * Configure associations that are applied to the generated
     * Output events, see {@link Event#setAssociated}.
     * 
     * @param associations the associations to apply
     * @return this object for easy chaining
     */
    public AbstractBufferWriter<B>
            setEventAssociations(Map<Object, Object> associations) {
        eventAssociations = associations;
        return this;
    }

    /**
     * Ensure that a buffer for output data is available.
     *
     * @throws InterruptedException the interrupted exception
     */
    protected abstract void ensureBufferAvailable() throws InterruptedException;

    @Override
    public abstract void write(char[] data, int offset, int length);

    @Override
    public void write(char[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }

    @Override
    public abstract void write(String str, int offset, int length);

    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }

    @Override
    @SuppressWarnings("PMD.ShortVariable")
    public void write(int ch) {
        char[] buff = { (char) ch };
        write(buff, 0, 1);
    }

    /**
     * Creates and fires an {@link Output} event with the buffer being filled. 
     * The end of record flag of the event is set according to the parameter.
     * Frees any allocated buffer.
     */
    protected void flush(boolean endOfRecord) {
        if (buffer == null) {
            if (!endOfRecord || eorSent) {
                return;
            }
            try {
                ensureBufferAvailable();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (buffer.position() == 0 && (!endOfRecord || eorSent)) {
            // Nothing to flush
            buffer.unlockBuffer();
        } else {
            if (sendInputEvents) {
                eventPipeline.fire(
                    associate(Input.fromSink(buffer, endOfRecord)), channel);
            } else {
                eventPipeline.fire(
                    associate(Output.fromSink(buffer, endOfRecord)), channel);
            }
            eorSent = endOfRecord;
        }
        buffer = null;
    }

    /**
     * Creates and fires a {@link Output} event with the buffer being filled
     * if it contains any data.
     * 
     * By default, the {@link Output} event is created with the end of record
     * flag set (see {@link Output#isEndOfRecord()}) in order to forward the 
     * flush as event. This implies that an {@link Output} event with no data
     * (but the end of record flag set) may be fired. This behavior can
     * be disabled with {@link #suppressEndOfRecord()}.
     */
    @Override
    public void flush() {
        flush(sendEor);
    }

    /**
     * Flushes any remaining data with the end of record flag set
     * (unless {@link #suppressEndOfRecord()} has been called)
     * and fires a {@link Close} event (unless {@link #suppressClose()}
     * has been called).
     */
    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        flush(sendEor);
        if (sendClose) {
            eventPipeline.fire(associate(new Close()), channel);
        }
        isClosed = true;
    }

    private Event<?> associate(Event<?> event) {
        if (eventAssociations != null) {
            for (var entry : eventAssociations.entrySet()) {
                event.setAssociated(entry.getKey(), entry.getValue());
            }
        }
        return event;
    }
}
