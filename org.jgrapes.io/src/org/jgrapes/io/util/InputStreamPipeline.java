/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Output;

/**
 * Forwards the content of an input stream as a sequence of {@link Output}
 * events.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class InputStreamPipeline implements Runnable {

    private InputStream inStream;
    private IOSubchannel channel;
    private EventPipeline eventPipeline;
    private boolean sendClose = true;

    /**
     * Creates a new pipeline that sends the data from the given input stream
     * as events on the given channel, using the given event pipeline.
     * 
     * @param in the input stream to read from
     * @param channel the channel to send to
     * @param eventPipeline
     *            the event pipeline used for firing events
     */
    @SuppressWarnings("PMD.ShortVariable")
    public InputStreamPipeline(InputStream in, IOSubchannel channel,
            EventPipeline eventPipeline) {
        this.inStream = in;
        this.channel = channel;
        this.eventPipeline = eventPipeline;
    }

    /**
     * Creates a new pipeline that sends the data from the given input stream
     * as events on the given channel, using the channel's response pipeline.
     * 
     * @param in the input stream to read from
     * @param channel the channel to send to
     */
    @SuppressWarnings("PMD.ShortVariable")
    public InputStreamPipeline(InputStream in, IOSubchannel channel) {
        this(in, channel, channel.responsePipeline());
    }

    /**
     * Suppresses the sending of a close event when the stream is closed. 
     * 
     * @return the stream for easy chaining
     */
    public InputStreamPipeline suppressClose() {
        sendClose = false;
        return this;
    }

    @Override
    public void run() {
        try (ReadableByteChannel inChannel = Channels.newChannel(inStream)) {
            ManagedBuffer<ByteBuffer> lookAhead = ManagedBuffer.wrap(
                ByteBuffer.allocate(channel.byteBufferPool().bufferSize()));
            // First attempt
            if (lookAhead.fillFromChannel(inChannel) == -1) {
                ManagedBuffer<ByteBuffer> buffer
                    = channel.byteBufferPool().acquire();
                eventPipeline.fire(Output.fromSink(buffer, true), channel);
            } else {
                while (true) {
                    // Save data read so far
                    ManagedBuffer<ByteBuffer> buffer
                        = channel.byteBufferPool().acquire();
                    buffer.linkBackingBuffer(lookAhead);
                    // Get new look ahead
                    lookAhead = ManagedBuffer.wrap(ByteBuffer.allocate(
                        channel.byteBufferPool().bufferSize()));
                    // Next read attempt
                    boolean eof;
                    try {
                        eof = lookAhead.fillFromChannel(inChannel) == -1;
                    } catch (IOException e) {
                        buffer.unlockBuffer();
                        throw e;
                    }
                    // Fire "old" data with up-to-date end of record flag.
                    eventPipeline.fire(Output.fromSink(buffer, eof), channel);
                    if (eof) {
                        break;
                    }
                }
            }
            if (sendClose) {
                eventPipeline.fire(new Close(), channel);
            }
        } catch (InterruptedException e) {
            // Just stop
        } catch (IOException e) {
            eventPipeline.fire(new IOError(null, e), channel);
        }
    }
}