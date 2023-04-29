/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;

/**
 * Forwards the lines from a {@link BufferedReader} as a sequence of 
 * {@link Output} (or optionally {@link Input}) events.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class BufferedReaderPipeline implements Runnable {

    private BufferedReader reader;
    private IOSubchannel channel;
    private EventPipeline eventPipeline;
    private boolean sendClosed = true;
    private Map<Object, Object> eventAssociations;
    private boolean sendInputEvents;

    /**
     * Creates a new pipeline that sends the lines from the given reader
     * as events on the given channel, using the given event pipeline.
     * 
     * @param in the reader to read from
     * @param channel the channel to send to
     * @param eventPipeline the event pipeline used for firing events
     */
    @SuppressWarnings("PMD.ShortVariable")
    public BufferedReaderPipeline(BufferedReader in, IOSubchannel channel,
            EventPipeline eventPipeline) {
        this.reader = in;
        this.channel = channel;
        this.eventPipeline = eventPipeline;
    }

    /**
     * Creates a new pipeline that sends the lines from the given reader
     * as events on the given channel, using the channel's response pipeline.
     * 
     * @param in the reader to read from
     * @param channel the channel to send to
     */
    @SuppressWarnings("PMD.ShortVariable")
    public BufferedReaderPipeline(BufferedReader in, IOSubchannel channel) {
        this(in, channel, channel.responsePipeline());
    }

    /**
     * Causes the data to be fired as {@link Input} events rather
     * than the usual {@link Output} events. 
     * 
     * @return the stream for easy chaining
     */
    public BufferedReaderPipeline sendInputEvents() {
        sendInputEvents = true;
        return this;
    }

    /**
     * Suppresses the sending of a closed event when the stream is closed. 
     * 
     * @return the stream for easy chaining
     */
    public BufferedReaderPipeline suppressClosed() {
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
    public BufferedReaderPipeline
            setEventAssociations(Map<Object, Object> associations) {
        eventAssociations = associations;
        return this;
    }

    @Override
    public void run() {
        try (var out = new CharBufferWriter(channel, eventPipeline)) {
            if (sendInputEvents) {
                out.sendInputEvents();
            }
            if (!sendClosed) {
                out.suppressClose();
            }
            out.setEventAssociations(eventAssociations);
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                out.write(line);
                out.flush();
            }
        } catch (IOException e) {
            eventPipeline.fire(associate(new IOError(null, e)), channel);
        }
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