/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.io.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Objects;

/**
 * A {@link Reader} that provides the data from the {@link ManagedBuffer}s
 * fed to it to a consumer. This class is intended to be used as a pipe 
 * between two threads.  
 */
public class ManagedBufferReader extends Reader {

    private boolean isEndOfFeed;
    private boolean isOpen = true;
    private ManagedBuffer<CharBuffer> current;

    /**
     * Feed data to the reader. The call blocks while data from a previous
     * invocation has not been fully read. The buffer passed as argument
     * is locked (see {@link ManagedBuffer#lockBuffer()}) until all
     * data has been read.
     * 
     * Calling this method with `null` as argument closes the feed.
     * After consuming any data still available from a previous
     * invocation, further calls to {@link #read} return -1.
     *
     * @param buffer the buffer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void feed(ManagedBuffer<CharBuffer> buffer) throws IOException {
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
    @SuppressWarnings("PMD.PreserveStackTrace")
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
            int transferred;
            if (current.remaining() <= len) {
                // Get all remaining.
                transferred = current.remaining();
                current.backingBuffer().get(cbuf, off, transferred);
                current.unlockBuffer();
                current = null;
                lock.notifyAll();
            } else {
                // Get requested.
                transferred = len;
                current.backingBuffer().get(cbuf, off, transferred);
            }
            return transferred;
        }
    }

}
