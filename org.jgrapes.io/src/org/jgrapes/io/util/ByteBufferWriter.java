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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;

/**
 * An {@link Writer} that encodes the data written to it and stores it
 * in a {@link ByteBuffer} obtained from a queue. When a byte buffer 
 * is full, an {@link Output} event (default) is generated and a 
 * new buffer is fetched from the queue.
 * 
 * The function of this class can also be achieved by wrapping a
 * {@link ByteBufferOutputStream} in a {@link OutputStreamWriter}.
 * The major advantage of this class is that it drops the
 * {@link IOException}s (which cannot happen) from the methods.
 * Besides, it should be more resource efficient.
 */
public class ByteBufferWriter extends AbstractBufferWriter<ByteBuffer> {

    private CharBuffer written;
    private Charset charset = StandardCharsets.UTF_8;
    private CharsetEncoder encoder;

    /**
     * Creates a new instance that uses {@link Output} events to dispatch
     * buffers on the given channel, using the given event pipeline.
     * 
     * @param channel
     *            the channel to fire events on
     * @param eventPipeline
     *            the event pipeline used for firing events
     */
    public ByteBufferWriter(IOSubchannel channel, EventPipeline eventPipeline) {
        super(channel, eventPipeline);
    }

    /**
     * Creates a new instance that uses {@link Output} events to dispatch
     * buffers on the given channel, using the channel's response pipeline.
     * 
     * @param channel the channel to fire events on
     */
    public ByteBufferWriter(IOSubchannel channel) {
        super(channel);
    }

    @Override
    public ByteBufferWriter sendInputEvents() {
        super.sendInputEvents();
        return this;
    }

    @Override
    public ByteBufferWriter suppressClose() {
        super.suppressClose();
        return this;
    }

    @Override
    public ByteBufferWriter suppressEndOfRecord() {
        super.suppressEndOfRecord();
        return this;
    }

    @Override
    public ByteBufferWriter
            setEventAssociations(Map<Object, Object> associations) {
        super.setEventAssociations(associations);
        return this;
    }

    /**
     * Sets the charset to be used for converting the written data
     * to bytes. Defaults to UTF-8. Must be set before the first 
     * invocation of any write method.  
     *
     * @param charset the charset
     * @return the managed buffer reader
     */
    public ByteBufferWriter charset(Charset charset) {
        if (encoder != null) {
            throw new IllegalStateException("Charset cannot be changed.");
        }
        this.charset = charset;
        return this;
    }

    /**
     * Sets the charset to be used for converting the written data
     * to bytes to the charset specified as system property 
     * `native.encoding`. If this property does not specify a valid 
     * charset, {@link Charset#defaultCharset()} is used.
     *  
     * Must be invoked before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @param charset the charset
     * @return the managed buffer reader
     */
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.EmptyCatchBlock", "PMD.DataflowAnomalyAnalysis" })
    public ByteBufferWriter nativeCharset() {
        Charset toSet = Charset.defaultCharset();
        var toCheck = System.getProperty("native.encoding");
        if (toCheck != null) {
            try {
                toSet = Charset.forName(toCheck);
            } catch (Exception e) {
                // If this fails, simply use default
            }
        }
        charset(toSet);
        return this;
    }

    @Override
    protected void ensureBufferAvailable() throws InterruptedException {
        if (buffer != null) {
            return;
        }
        buffer = channel.byteBufferPool().acquire();
    }

    private void ensureWrittenAvailable() throws InterruptedException {
        if (written == null) {
            ensureBufferAvailable();
            written = CharBuffer.allocate(buffer.capacity());
            encoder = charset.newEncoder();
        }
    }

    private void encode() throws InterruptedException {
        written.flip();
        while (true) {
            ensureBufferAvailable();
            var res = encoder.encode(written, buffer.backingBuffer(), false);
            if (res.isUnderflow()) {
                // This should not be possible (incomplete character?).
                var carryOver = CharBuffer.allocate(written.capacity());
                carryOver.put(written);
                written = carryOver;
                return;
            }
            if (!res.isOverflow()) {
                break;
            }
            flush(false);
        }
        // written processed
        written.clear();
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
                ensureWrittenAvailable();
                if (written.remaining() >= length) {
                    written.put(data, offset, length);
                    encode();
                    break;
                }
                int chunkSize = written.remaining();
                written.put(data, offset, chunkSize);
                length -= chunkSize;
                offset += chunkSize;
                encode();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
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
                ensureWrittenAvailable();
                if (written.remaining() >= length) {
                    str.getChars(offset, offset + length, written.array(),
                        written.position());
                    written.position(written.position() + length);
                    encode();
                    break;
                }
                int chunkSize = buffer.remaining();
                str.getChars(offset, offset + chunkSize, written.array(),
                    written.position());
                written.position(written.position() + chunkSize);
                length -= chunkSize;
                offset += chunkSize;
                encode();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
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
    public ByteBufferWriter append(char ch) {
        write(ch);
        return this;
    }

    @Override
    public ByteBufferWriter append(CharSequence csq) {
        write(String.valueOf(csq));
        return this;
    }

    @Override
    public ByteBufferWriter append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        return append(csq.subSequence(start, end));
    }

}
