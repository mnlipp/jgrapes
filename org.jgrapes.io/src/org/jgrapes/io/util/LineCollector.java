/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2023 Michael N. Lipp
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.jgrapes.io.events.Input;

/**
 * Collects character data from buffers and makes it available as
 * complete lines.
 * 
 * Lines end with a LF which may optionally be followed by a CR.
 * Neither character is part of the result returned by {@link #getLine()}.
 * If no more input is expected and characters without trailing LF
 * remain, these remaining character are returned as a line as well.   
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class LineCollector {
    private boolean isEof;
    private CharsetDecoder decoder;
    private Charset charset = StandardCharsets.UTF_8;
    private CharBuffer pending;
    private CharBuffer rest;
    private boolean endedWithLF;
    private final Queue<String> lines = new ConcurrentLinkedQueue<>();
    private Consumer<String> consumer = s -> lines.add(s);

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>`. Defaults to UTF-8. 
     * Must be set before the first invocation of 
     * {@link #feed(ManagedBuffer)}.  
     *
     * @param charset the charset
     * @return the managed buffer reader
     */
    public LineCollector charset(Charset charset) {
        if (decoder != null) {
            throw new IllegalStateException("Charset cannot be changed.");
        }
        this.charset = charset;
        return this;
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
        "PMD.EmptyCatchBlock" })
    public LineCollector nativeCharset() {
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
     * Configures a consumer for lines. The consumer is invoked when
     * a complete line has been detected. If a consumer is configured,
     * {@link #getLine()} may not be used (always returns `null`).
     *
     * @param consumer the consumer
     * @return the line collector
     */
    public LineCollector consumer(Consumer<String> consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * Feed data to the collector. 
     * 
     * Calling this method with `null` as argument closes the feed.
     *
     * @param buffer the buffer
     */
    public <W extends Buffer> void feed(W buffer) {
        if (isEof) {
            return;
        }
        if (buffer == null) {
            isEof = true;
        } else {
            copyToPending(buffer);
        }
        extractLines();

    }

    /**
     * Feed data to the collector. 
     * 
     * Calling this method with `null` as argument closes the feed.
     *
     * @param buffer the buffer
     */
    public <W extends Buffer> void feed(ManagedBuffer<W> buffer) {
        if (buffer == null) {
            feed((W) null);
        } else {
            feed(buffer.backingBuffer());
        }
    }

    /**
     * Feed data to the collector. 
     * 
     * Calling this method with `null` as argument closes the feed.
     *
     * @param event the event
     */
    public <W extends Buffer> void feed(Input<W> event) {
        if (event == null) {
            feed((W) null);
        } else {
            feed(event.buffer());
        }
    }

    private <W extends Buffer> void copyToPending(W buffer) {
        try {
            buffer.mark();
            if (pending == null) {
                pending = CharBuffer.allocate(buffer.capacity());
            }
            if (buffer instanceof CharBuffer charBuf) {
                if (pending.remaining() < charBuf.remaining()) {
                    resizePending(charBuf);
                }
                pending.put(charBuf);
                return;
            }
            if (decoder == null) {
                decoder = charset.newDecoder();
            }
            while (true) {
                var result
                    = decoder.decode((ByteBuffer) buffer, pending, isEof);
                if (!result.isOverflow()) {
                    break;
                }
                // Need larger buffer
                resizePending(buffer);
            }
        } finally {
            buffer.reset();
        }
    }

    private void resizePending(Buffer toAppend) {
        var old = pending;
        pending = CharBuffer.allocate(old.capacity() + toAppend.capacity());
        old.flip();
        pending.put(old);
    }

    @SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NcssCount",
        "PMD.NPathComplexity", "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidReassigningLoopVariables",
        "PMD.AvoidBranchingStatementAsLastInLoop", "PMD.CyclomaticComplexity",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    private void extractLines() {
        pending.flip();
        if (!pending.hasRemaining()) {
            pending.clear();
            return;
        }
        if (endedWithLF && pending.get(pending.position()) == '\r') {
            pending.get();
        }
        int end = pending.limit();
        endedWithLF = false;
        while (pending.hasRemaining()) {
            int start = pending.position();
            for (int pos = start; pos < end;) {
                if (pending.get(pos) != '\n') {
                    pos += 1;
                    continue;
                }
                consumer
                    .accept(new String(pending.array(), start, pos - start));
                pos += 1;
                endedWithLF = pos >= end;
                if (pos < end && pending.get(pos) == '\r') {
                    pos += 1;
                }
                pending.position(pos);
                break;
            }
            if (pending.position() == start) {
                // No LF found
                break;
            }
        }
        if (!pending.hasRemaining()) {
            // Last input was or ended with complete line
            pending.clear();
            return;
        }
        // Incomplete line
        if (isEof) {
            consumer.accept(new String(pending.array(), pending.position(),
                pending.remaining()));
            return;
        }
        if (pending.position() == 0) {
            // Nothing consumed, continue to write into pending
            var limit = pending.limit();
            pending.clear();
            pending.position(limit);
            return;
        }
        // Transfer remaining to beginning of pending
        if (rest == null || rest.capacity() < pending.remaining()) {
            rest = CharBuffer.allocate(pending.capacity());
        }
        rest.put(pending);
        rest.flip();
        pending.clear();
        pending.put(rest);
        rest.clear();
    }

    /**
     * Checks if more input may become available.
     *
     * @return true, if successful
     */
    public boolean eof() {
        return isEof;
    }

    /**
     * Gets the next line.
     *
     * @return the line
     */
    public String getLine() {
        return lines.poll();
    }
}
