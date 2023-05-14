/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022, 2023 Michael N. Lipp
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
import java.io.InterruptedIOException;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A {@link Reader} that provides the data from the {@link ManagedBuffer}s
 * fed to it to a consumer. This class is intended to be used as a pipe 
 * between two threads.  
 */
public class ManagedBufferReader extends Reader {

    private boolean isEndOfFeed;
    private boolean isOpen = true;
    private ManagedBuffer<? extends Buffer> current;
    private CharsetDecoder decoder;
    private CharBuffer decoded;
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>`. Defaults to UTF-8. 
     * Must be set before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @param charset the charset
     * @return the managed buffer reader
     */
    public ManagedBufferReader charset(Charset charset) {
        if (decoder != null) {
            throw new IllegalStateException("Charset cannot be changed.");
        }
        this.charset = charset;
        return this;
    }

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>`. Defaults to UTF-8. 
     * Must be set before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @param charset the charset
     * @return the managed buffer reader
     * @deprecated Use {@link #charset(Charset)} instead
     */
    @Deprecated
    public ManagedBufferReader setCharset(Charset charset) {
        return charset(charset);
    }

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>` to the charset
     * specified as system property `native.encoding`. If this
     * property does not specify a valid charset, 
     * {@link Charset#defaultCharset()} is used.
     *  
     * Must be invoked before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @return the managed buffer reader
     */
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.EmptyCatchBlock", "PMD.DataflowAnomalyAnalysis" })
    public ManagedBufferReader nativeCharset() {
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

    /**
     * Feed data to the reader. The call blocks while data from a previous
     * invocation has not been fully read. The buffer passed as argument
     * is locked (see {@link ManagedBuffer#lockBuffer()}) until all
     * data has been read.
     * 
     * Calling this method with `null` as argument closes the feed.
     * After consuming any data still available from a previous
     * invocation, further calls to {@link #read} therefore return -1.
     *
     * @param buffer the buffer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings({ "PMD.PreserveStackTrace" })
    public <W extends Buffer> void feed(ManagedBuffer<W> buffer)
            throws IOException {
        synchronized (lock) {
            if (buffer == null) {
                isEndOfFeed = true;
                notifyAll();
                return;
            }
            if (!isOpen || isEndOfFeed) {
                return;
            }
            while (current != null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    var exc = new InterruptedIOException(e.getMessage());
                    exc.setStackTrace(e.getStackTrace());
                    throw exc;
                }
            }
            current = buffer;
            buffer.lockBuffer();
            lock.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Note that this is the {@link Reader}'s `close` method. In order
     * to close the feed, call {@link #feed(ManagedBuffer)} with
     * `null` as argument.
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            isOpen = false;
            if (current != null) {
                current.unlockBuffer();
                current = null;
            }
            lock.notifyAll();
        }
    }

    @Override
    @SuppressWarnings({ "PMD.PreserveStackTrace", "unchecked",
        "PMD.CognitiveComplexity", "PMD.DataflowAnomalyAnalysis",
        "PMD.NcssCount" })
    public int read(char[] cbuf, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, cbuf.length);
        synchronized (lock) {
            while (isOpen && current == null && !isEndOfFeed) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    var exc = new InterruptedIOException(e.getMessage());
                    exc.setStackTrace(e.getStackTrace());
                    throw exc;
                }
            }
            if (!isOpen || isEndOfFeed && current == null) {
                return -1;
            }
            CharBuffer input;
            if (current.backingBuffer() instanceof CharBuffer) {
                input = ((ManagedBuffer<CharBuffer>) current).backingBuffer();
            } else {
                if (decoder == null) {
                    decoder = charset.newDecoder();
                    decoded = CharBuffer.allocate(current.capacity());
                }
                var result = decoder.decode(
                    ((ManagedBuffer<ByteBuffer>) current).backingBuffer(),
                    decoded, isEndOfFeed);
                assert !result.isOverflow();
                decoded.flip();
                input = decoded;
            }
            int transferred;
            if (input.remaining() <= len) {
                // Get all remaining.
                transferred = input.remaining();
                input.get(cbuf, off, transferred);
                if (decoded != null) {
                    decoded.clear();
                }
                current.unlockBuffer();
                current = null;
                lock.notifyAll();
            } else {
                // Get requested.
                transferred = len;
                input.get(cbuf, off, transferred);
            }
            return transferred;
        }
    }

}
