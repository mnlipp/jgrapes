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

package org.jgrapes.http;

import java.net.URLDecoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.InputConsumer;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * Decodes www-form-urlencoded data.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class WwwFormUrldecoder implements InputConsumer {
    private boolean isEof;
    private CharsetDecoder decoder;
    private Charset charset = StandardCharsets.UTF_8;
    private Charset formCharset = StandardCharsets.UTF_8;
    private CharBuffer pending;
    private CharBuffer rest;
    private final Map<String, List<String>> result = new ConcurrentHashMap<>();
    private BiConsumer<String, String> consumer = (k, v) -> {
        result.computeIfAbsent(k,
            key -> Collections.synchronizedList(new ArrayList<>())).add(v);
    };

    /**
     * Sets the charset to be used if {@link #feed(ManagedBuffer)}
     * is invoked with `ManagedBuffer<ByteBuffer>`. Defaults to UTF-8. 
     * Must be set before the first invocation of {@link #feed(ManagedBuffer)}.
     * 
     * This is provided for edge cases. As "urlencoded" data may only
     * contain ASCII characters, it does not make sense to specify
     * a charset for media type `x-www-form-urlencoded`
     *
     * @param charset the charset
     * @return the managed buffer reader
     */
    public WwwFormUrldecoder charset(Charset charset) {
        if (decoder != null) {
            throw new IllegalStateException("Charset cannot be changed.");
        }
        this.charset = charset;
        return this;
    }

    /**
     * The charset to be used with {@link URLDecoder#decode(String, Charset)}.
     * Defaults to UTF-8.
     *
     * @param charset the charset
     * @return the www form urldecoder
     */
    public WwwFormUrldecoder formCharset(Charset charset) {
        formCharset = charset;
        return this;
    }

    /**
     * Configures a consumer for key/value pairs. The consumer is invoked
     * when a pair has been decoded. If a consumer is configured,
     * {@link #result()} must not be used (always returns an empty map).
     *
     * @param consumer the consumer
     * @return the decoder
     */
    public WwwFormUrldecoder consumer(BiConsumer<String, String> consumer) {
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
    public <W extends Buffer> void feed(ManagedBuffer<W> buffer) {
        if (buffer == null) {
            isEof = true;
        } else {
            copyToPending(buffer.backingBuffer());
        }
        processPending();
    }

    /**
     * Calls {@link #feed(ManagedBuffer)} with the provided event's
     * buffer. If {@link Input#isEndOfRecord()} returns `true`,
     * no further input data is processed.
     * 
     * Calling this method with `null` indicates the end of the feed.
     *
     * @param <W> the generic type
     * @param event the event
     */
    @Override
    public <W extends Buffer> void feed(Input<W> event) {
        if (event == null) {
            feed((ManagedBuffer<W>) null);
        } else {
            feed(event.buffer());
            if (event.isEndOfRecord()) {
                isEof = true;
            }
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

    @SuppressWarnings({ "PMD.AvoidReassigningLoopVariables",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidBranchingStatementAsLastInLoop", "PMD.NcssCount",
        "PMD.NPathComplexity" })
    private void processPending() {
        pending.flip();
        if (!pending.hasRemaining()) {
            pending.clear();
            return;
        }
        int end = pending.limit();
        while (pending.hasRemaining()) {
            int start = pending.position();
            for (int pos = start; pos < end;) {
                if (pending.get(pos) != '&') {
                    pos += 1;
                    continue;
                }
                splitPair(new String(pending.array(), start, pos - start));
                pos += 1;
                pending.position(pos);
                break;
            }
            if (pending.position() == start) {
                // No '&' found
                break;
            }
        }
        if (!pending.hasRemaining()) {
            pending.clear();
            return;
        }
        if (isEof) {
            // Remaining is last entry
            splitPair(new String(pending.array(), pending.position(),
                pending.remaining()).trim());
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

    private void splitPair(String pair) {
        int sep = pair.indexOf('=');
        String key = URLDecoder.decode(pair.substring(0, sep), formCharset);
        String value = URLDecoder.decode(pair.substring(sep + 1), formCharset);
        consumer.accept(key, value);
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
     * Gets the result.
     *
     * @return the line
     */
    public Map<String, List<String>> result() {
        return result;
    }
}
