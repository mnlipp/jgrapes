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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.util.events.DataInput;
import org.jgrapes.io.util.events.JsonParsingError;

/**
 * A {@link ManagedBufferStreamer} that feeds the data to a JSON parser.
 * When the data is fully parsed, it is made available by firing a
 * {@link DataInput} event.
 * 
 * @since 2.8
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class JsonReader extends ManagedBufferStreamer {

    /**
     * Instantiates a new JSON reader.
     *
     * @param <R> the result data type
     * @param mapper the mapper
     * @param resultType the result type
     * @param pipeline the pipeline to use for sending the
     * {@link DataInput} event
     * @param channel the channel to use for sending the 
     * {@link DataInput} event
     */
    public <R> JsonReader(ObjectMapper mapper, Class<R> resultType,
            EventPipeline pipeline, Channel channel) {
        super(r -> {
            try {
                pipeline.fire(new DataInput<R>(mapper.readValue(r, resultType)),
                    channel);
            } catch (Exception e) {
                pipeline.fire(new JsonParsingError(e), channel);
            }
        });
    }

    /**
     * Instantiates a new JSON reader that uses a default object mapper.
     *
     * @param <R> the result data type
     * @param resultType the result type
     * @param pipeline the pipeline to use for sending the
     * {@link DataInput} event
     * @param channel the channel to use for sending the
     * {@link DataInput} event
     */
    public <R> JsonReader(Class<R> resultType, EventPipeline pipeline,
            Channel channel) {
        this(new ObjectMapper(), resultType, pipeline, channel);
    }
}
