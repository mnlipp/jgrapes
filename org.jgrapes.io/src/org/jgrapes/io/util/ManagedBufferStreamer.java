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

import java.io.IOException;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import org.jgrapes.io.events.Input;

/**
 * Starts a thread with a synchronous consumer of input provided by a
 * {@link Reader} which is fed with data from {@link ManagedBuffer}s
 * (usually obtained from {@link Input} events).
 *  
 * @since 2.8.0
 */
public class ManagedBufferStreamer {

    private final ManagedBufferReader reader = new ManagedBufferReader();

    public ManagedBufferStreamer(Consumer<Reader> processor) {
        Thread thread = new Thread(() -> {
            processor.accept(reader);
        });
        thread.start();
        ThreadCleaner.watch(this, thread);
    }

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>`. Defaults to UTF-8. 
     * Must be set before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @param charset the charset
     * @return the managed buffer streamer
     */
    public ManagedBufferStreamer charset(Charset charset) {
        reader.charset(charset);
        return this;
    }

    /**
     * Feed data to underlying the reader
     * (see {@link ManagedBufferReader#feed). 
     * 
     * @param buffer the buffer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings({ "PMD.PreserveStackTrace" })
    public <W extends Buffer> void feed(ManagedBuffer<W> buffer)
            throws IOException {
        reader.feed(buffer);
    }

}
