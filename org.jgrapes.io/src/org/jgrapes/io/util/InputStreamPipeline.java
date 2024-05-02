/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2023  Michael N. Lipp
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.IOEvent;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;

/**
 * Forwards the content of an input stream as a sequence of 
 * {@link Output} (or optionally {@link Input}) events.
 * 
 * The default settings and the constructor 
 * {@link #InputStreamPipeline(InputStream, IOSubchannel)} reflect
 * the usage of this class for generating a response (e.g. provide
 * the content of a file in response to a request from a client).
 * Using the class with a "downstream" event pipeline, generating
 * {@link Input} events is used when an input stream generates events
 * that should be processed as requests by the application.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class InputStreamPipeline implements Runnable {

    private InputStream inStream;
    private IOSubchannel channel;
    private EventPipeline eventPipeline;
    private boolean sendClosed = true;
    private Map<Object, Object> eventAssociations;
    private boolean sendInputEvents;

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
     * Causes the data to be fired as {@link Input} events rather
     * than the usual {@link Output} events. 
     * 
     * @return the stream for easy chaining
     */
    public InputStreamPipeline sendInputEvents() {
        sendInputEvents = true;
        return this;
    }

    /**
     * Suppresses the sending of a closed event when the stream is closed. 
     * 
     * @return the stream for easy chaining
     */
    public InputStreamPipeline suppressClosed() {
        sendClosed = false;
        return this;
    }

    /**
     * Configure associations that are applied to the generated
     * Output events, see {@link Event#setAssociated}.
     * 
     * @param associations the associations to apply
     * @return the pipeline for easy chaining
     */
    public InputStreamPipeline
            setEventAssociations(Map<Object, Object> associations) {
        eventAssociations = associations;
        return this;
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public void run() {
        try {
            if (inStream instanceof FileInputStream fip) {
                seekableTransfer(fip.getChannel());
            } else {
                defaultTransfer();
            }
            if (sendClosed) {
                eventPipeline.fire(associate(new Closed<Void>()), channel);
            }
        } catch (InterruptedException e) { // NOPMD
            // Just stop
        } catch (IOException e) {
            eventPipeline.fire(associate(new IOError(null, e)), channel);
        }
    }

    private void defaultTransfer() throws InterruptedException, IOException {
        // If available() returns remaining, we can optimize.
        // Regrettably, there is no marker interface for this, but
        // the assumption should be true for ByteArrayInputStream.
        boolean availableIsRemaining = inStream instanceof ByteArrayInputStream;
        while (true) {
            ManagedBuffer<ByteBuffer> buffer = null;
            try {
                buffer = channel.byteBufferPool().acquire();
                var backing = buffer.backing;
                int recvd = inStream.read(backing.array(),
                    backing.position(), backing.remaining());
                if (recvd > 0) {
                    boolean eof
                        = availableIsRemaining && inStream.available() == 0;
                    backing.position(backing.position() + recvd);
                    eventPipeline.fire(associate(ioEvent(buffer, eof)),
                        channel);
                    if (eof) {
                        break;
                    }
                    continue;
                }
                if (recvd == -1) {
                    eventPipeline.fire(associate(ioEvent(buffer, true)),
                        channel);
                    break;
                }
                // Reading 0 bytes shouldn't happen.
                buffer.unlockBuffer();
            } catch (IOException e) {
                buffer.unlockBuffer();
                throw e;
            }
        }
    }

    /**
     * A seekable channel allows us to avoid generating an event with
     * no data and eof set, because we can check after reading if there
     * is remaining data.
     *
     * @param input the input
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void seekableTransfer(SeekableByteChannel input)
            throws InterruptedException, IOException {
        while (true) {
            ManagedBuffer<ByteBuffer> buffer = null;
            try {
                buffer = channel.byteBufferPool().acquire();
                int recvd = input.read(buffer.backing);
                if (recvd > 0) {
                    boolean eof = input.position() == input.size();
                    eventPipeline.fire(associate(ioEvent(buffer, eof)),
                        channel);
                    if (eof) {
                        break;
                    }
                    continue;
                }
                if (recvd == -1) {
                    eventPipeline.fire(associate(ioEvent(buffer, true)),
                        channel);
                    break;
                }
                // Reading 0 bytes shouldn't happen.
                buffer.unlockBuffer();
            } catch (IOException e) {
                buffer.unlockBuffer();
                throw e;
            }
        }
    }

    private IOEvent<ByteBuffer> ioEvent(ManagedBuffer<ByteBuffer> buffer,
            boolean eor) {
        if (sendInputEvents) {
            return Input.fromSink(buffer, eor);
        }
        return Output.fromSink(buffer, eor);
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