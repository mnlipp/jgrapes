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
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;

/**
 * An {@link Writer} that is backed by {@link CharBuffer}s obtained from a
 * queue. When a byte buffer is full, an {@link Output} event (default) is
 * generated and a new buffer is fetched from the queue.
 */
public class CharBufferWriter extends AbstractBufferWriter<CharBuffer> {

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
        super(channel, eventPipeline);
    }

    /**
     * Creates a new instance that uses {@link Output} events to dispatch
     * buffers on the given channel, using the channel's response pipeline.
     * 
     * @param channel the channel to fire events on
     */
    public CharBufferWriter(IOSubchannel channel) {
        super(channel);
    }

    @Override
    public CharBufferWriter sendInputEvents() {
        super.sendInputEvents();
        return this;
    }

    @Override
    public CharBufferWriter suppressClose() {
        super.suppressClose();
        return this;
    }

    @Override
    public CharBufferWriter suppressEndOfRecord() {
        super.suppressEndOfRecord();
        return this;
    }

    @Override
    public CharBufferWriter
            setEventAssociations(Map<Object, Object> associations) {
        super.setEventAssociations(associations);
        return this;
    }

    /**
     * Ensure that a buffer for output data is available.
     *
     * @throws InterruptedException the interrupted exception
     */
    @Override
    protected void ensureBufferAvailable() throws InterruptedException {
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
            } else if (buf.remaining() == length) {
                buf.put(data, offset, length);
                flush(false);
                break;
            } else {
                int chunkSize = buf.remaining();
                buf.put(data, offset, chunkSize);
                flush(false);
                length -= chunkSize;
                offset += chunkSize;
            }
        }
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
            int chunkSize = buf.remaining();
            str.getChars(offset, offset + chunkSize, buf.array(),
                buf.position());
            buf.position(buf.position() + chunkSize);
            flush(false);
            length -= chunkSize;
            offset += chunkSize;
        }
    }

    @Override
    @SuppressWarnings("PMD.ShortVariable")
    public CharBufferWriter append(char ch) {
        write(ch);
        return this;
    }

    @Override
    public CharBufferWriter append(CharSequence csq) {
        write(String.valueOf(csq));
        return this;
    }

    @Override
    public CharBufferWriter append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        return append(csq.subSequence(start, end));
    }

}
