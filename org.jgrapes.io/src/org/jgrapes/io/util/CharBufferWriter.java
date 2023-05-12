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
import java.nio.CharBuffer;
import java.util.Map;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;

/**
 * An {@link Writer} that is backed by {@link CharBuffer}s obtained from a
 * queue. When a byte buffer is full, an {@link Output} event (default) is
 * generated and a new buffer is fetched from the queue.
 */
public class CharBufferWriter extends Writer {

    private IOSubchannel channel;
    private EventPipeline eventPipeline;
    private boolean sendInputEvents;
    private ManagedBuffer<CharBuffer> buffer;
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
    public CharBufferWriter(IOSubchannel channel, EventPipeline eventPipeline) {
        this.channel = channel;
        this.eventPipeline = eventPipeline;
    }

    /**
     * Creates a new instance that uses {@link Output} events to dispatch
     * buffers on the given channel, using the channel's response pipeline.
     * 
     * @param channel the channel to fire events on
     */
    public CharBufferWriter(IOSubchannel channel) {
        this(channel, channel.responsePipeline());
    }

    /**
     * Causes the data to be fired as {@link Input} events rather
     * than the usual {@link Output} events. 
     * 
     * @return the stream for easy chaining
     */
    public CharBufferWriter sendInputEvents() {
        sendInputEvents = true;
        return this;
    }

    /**
     * Suppresses sending of a close event when the stream is closed. 
     * 
     * @return the stream for easy chaining
     */
    public CharBufferWriter suppressClose() {
        sendClose = false;
        return this;
    }

    /**
     * Suppresses setting the end of record flag when the stream is 
     * flushed or closed.
     * 
     * @return the stream for easy chaining
     * @see Output#isEndOfRecord()
     */
    public CharBufferWriter suppressEndOfRecord() {
        sendEor = false;
        return this;
    }

    /**
     * Configure associations that are applied to the generated
     * Output events, see {@link Event#setAssociated}.
     * 
     * @param associations the associations to apply
     * @return the pipeline for easy chaining
     */
    public CharBufferWriter
            setEventAssociations(Map<Object, Object> associations) {
        eventAssociations = associations;
        return this;
    }

    private void ensureBufferAvailable() throws InterruptedException {
        if (buffer != null) {
            return;
        }
        buffer = channel.charBufferPool().acquire();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(char[] data, int offset, int length) {
        while (true) {
            try {
                ensureBufferAvailable();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            var buf = buffer.backingBuffer();
            if (buf.remaining() > length) {
                buf.put(data, offset, length);
                break;
            } else if (buffer.remaining() == length) {
                buf.put(data, offset, length);
                flush(false);
                break;
            } else {
                int chunkSize = buffer.remaining();
                buf.put(data, offset, chunkSize);
                flush(false);
                length -= chunkSize;
                offset += chunkSize;
            }
        }
    }

    @Override
    public void write(char[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }

    @Override
    public void write(String str, int offset, int length) {
        while (true) {
            try {
                ensureBufferAvailable();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            var buf = buffer.backingBuffer();
            if (buf.remaining() >= length) {
                str.getChars(offset, offset + length, buf.array(),
                    buf.position());
                buf.position(buf.position() + length);
                if (buf.remaining() == 0) {
                    flush(false);
                }
                break;
            }
            int chunkSize = buffer.remaining();
            str.getChars(offset, offset + chunkSize, buf.array(),
                buf.position());
            buf.position(buf.position() + chunkSize);
            flush(false);
            length -= chunkSize;
            offset += chunkSize;
        }
    }

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

    @Override
    @SuppressWarnings("PMD.ShortVariable")
    public Writer append(char ch) {
        write(ch);
        return this;
    }

    @Override
    public Writer append(CharSequence csq) {
        write(String.valueOf(csq));
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        return append(csq.subSequence(start, end));
    }

    /**
     * Creates and fires an {@link Output} event with the buffer being filled. 
     * The end of record flag of the event is set according to the parameter.
     * Frees any allocated buffer.
     */
    private void flush(boolean endOfRecord) {
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
