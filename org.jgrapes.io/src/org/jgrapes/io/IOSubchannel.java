/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapes.core.Associator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;

/**
 * Represents an I/O subchannel. Subchannels delegate the invocations of a
 * {@link Channel}'s methods to their respective main channel. Events fired on a
 * subchannel are therefore handled by the framework as if they were fired on
 * the main channel. Firing events on a subchannel instance instead of on the
 * main channel is a means to group related events. In particular, I/O
 * subchannels are used to group events that relate to an I/O resource such as
 * an opened file or a network connection.
 * 
 * A subchannel has an initiator that creates and manages the subchannel. Events
 * fired by the initiator are said to flow downstream on the channel. Events
 * fired by components in response are said to flow upstream.
 * 
 * Upstream and downstream events are usually handled by two different pipelines
 * managed by the initiator. One pipeline, accessible only to the initiator,
 * handles the downstream events. The other, made available as a property of the
 * I/O subchannel (see {@link #responsePipeline()} and {@link #respond(Event)}), 
 * handles the upstream events. Of course, any pipeline can be
 * used to send events upstream to the initiator component. However, using
 * arbitrary pipelines holds the risk that events aren't delivered in the
 * intended order.
 * 
 * An I/O subchannel also provides associated buffer pools for byte buffers
 * and character buffers. Buffers used in responses (upstream events)
 * should be acquired from these pools only. The initiator should initialize 
 * the pools in such a way that it suits its needs.
 */
public interface IOSubchannel extends Channel, Associator {

    /**
     * Returns the main channel.
     * 
     * @return the mainChannel
     */
    Channel mainChannel();

    /**
     * Returns the main channel's match value.
     * 
     * @see Channel#defaultCriterion()
     */
    @Override
    default Object defaultCriterion() {
        return mainChannel().defaultCriterion();
    }

    /**
     * Delegates to main channel.
     * 
     * @see Channel#isEligibleFor(Object)
     */
    @Override
    default boolean isEligibleFor(Object value) {
        return mainChannel().isEligibleFor(value);
    }

    /**
     * Gets the {@link EventPipeline} that can be used for events going back to
     * the initiator of this connection. Consistently using this event pipeline
     * for response events ensures that the events are written in proper
     * sequence.
     * 
     * @return the event pipeline
     */
    EventPipeline responsePipeline();

    /**
     * Get the subchannel's byte buffer pool.
     * 
     * @return the buffer pool
     */
    ManagedBufferPool<ManagedBuffer<ByteBuffer>, ByteBuffer>
            byteBufferPool();

    /**
     * Get the subchannel's char buffer pool.
     * 
     * @return the buffer pool
     */
    ManagedBufferPool<ManagedBuffer<CharBuffer>, CharBuffer>
            charBufferPool();

    /**
     * Fires the given event on this subchannel using the subchannel's response
     * pipeline. Effectively, {@code fire(someEvent)} is a shortcut for
     * {@code getResponsePipeline.add(someEvent, this)}.
     * 
     * @param <T> the event's type
     * @param event
     *            the event to fire
     * @return the event (for easy chaining)
     */
    default <T extends Event<?>> T respond(T event) {
        return responsePipeline().fire(event, this);
    }

    /**
     * Returns a string representation of the channel.
     *
     * @param subchannel the subchannel
     * @return the string
     */
    static String toString(IOSubchannel subchannel) {
        StringBuilder builder = new StringBuilder();
        builder.append(Channel.toString(subchannel.mainChannel()))
            .append('{')
            .append(Components.objectName(subchannel))
            .append('}');
        return builder.toString();
    }

    /**
     * Creates a new subchannel of the given component's channel with the
     * given event pipeline and a buffer pool with two buffers sized 4096.
     *
     * @param component the component used to get the main channel
     * @param responsePipeline the response pipeline
     * @return the subchannel
     */
    static IOSubchannel create(
            Component component, EventPipeline responsePipeline) {
        return new DefaultSubchannel(component.channel(), responsePipeline);
    }

    /**
     * A simple implementation of {@link IOSubchannel}.
     */
    class DefaultSubchannel implements IOSubchannel {
        private final Channel mainChannel;
        private final EventPipeline responsePipeline;
        private ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> byteBufferPool;
        private ManagedBufferPool<ManagedBuffer<CharBuffer>,
                CharBuffer> charBufferPool;
        private Map<Object, Object> contextData;

        /**
         * Creates a new instance with the given main channel and response
         * pipeline.  
         * 
         * @param mainChannel the main channel
         * @param responsePipeline the response pipeline to use
         * 
         */
        public DefaultSubchannel(
                Channel mainChannel, EventPipeline responsePipeline) {
            super();
            this.mainChannel = mainChannel;
            this.responsePipeline = responsePipeline;
        }

        protected void setByteBufferPool(
                ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                        ByteBuffer> bufferPool) {
            this.byteBufferPool = bufferPool;
        }

        protected void setCharBufferPool(
                ManagedBufferPool<ManagedBuffer<CharBuffer>,
                        CharBuffer> bufferPool) {
            this.charBufferPool = bufferPool;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.io.IOSubchannel#getMainChannel()
         */
        @Override
        public Channel mainChannel() {
            return mainChannel;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.io.IOSubchannel#responsePipeline()
         */
        @Override
        public EventPipeline responsePipeline() {
            return responsePipeline;
        }

        /**
         * Returns the buffer pool set. If no buffer pool has been set, a
         * buffer pool with with two buffers of size 4096 is created.
         */
        public ManagedBufferPool<ManagedBuffer<ByteBuffer>, ByteBuffer>
                byteBufferPool() {
            if (byteBufferPool == null) {
                byteBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
                    () -> {
                        return ByteBuffer.allocate(4096);
                    }, 2)
                        .setName(Components.objectName(this)
                            + ".upstream.byteBuffers");
            }
            return byteBufferPool;
        }

        /**
         * Returns the buffer pool set. If no buffer pool has been set, a
         * buffer pool with with two buffers of size 4096 is created.
         */
        public ManagedBufferPool<ManagedBuffer<CharBuffer>, CharBuffer>
                charBufferPool() {
            if (charBufferPool == null) {
                charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
                    () -> {
                        return CharBuffer.allocate(4096);
                    }, 2)
                        .setName(Components.objectName(this)
                            + ".upstream.charBuffers");
            }
            return charBufferPool;
        }

        /**
         * Establishes a "named" association to an associated object. Note that 
         * anything that represents an id can be used as value for 
         * parameter `name`, it does not necessarily have to be a string.
         * 
         * @param by the "name"
         * @param with the object to be associated
         */
        @SuppressWarnings("PMD.ShortVariable")
        public DefaultSubchannel setAssociated(Object by, Object with) {
            if (contextData == null) {
                contextData = new ConcurrentHashMap<>();
            }
            if (with == null) {
                contextData.remove(by);
            } else {
                contextData.put(by, with);
            }
            return this;
        }

        /**
         * Retrieves the associated object following the association 
         * with the given "name". This general version of the method
         * supports the retrieval of values of arbitrary types
         * associated by any "name" types. 
         * 
         * @param by the "name"
         * @param type the tape of the value to be retrieved
         * @param <V> the type of the value to be retrieved
         * @return the associate, if any
         */
        @SuppressWarnings("PMD.ShortVariable")
        public <V> Optional<V> associated(Object by, Class<V> type) {
            if (contextData == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(contextData.get(by))
                .filter(found -> type.isAssignableFrom(found.getClass()))
                .map(match -> type.cast(match));
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return IOSubchannel.toString(this);
        }

    }
}
