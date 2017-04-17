/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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

package org.jgrapes.io;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;

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
 * I/O subchannel, handles the upstream events. Of course, any pipeline can be
 * used to send events upstream to the initiator component. However, using
 * arbitrary pipelines holds the risk that events aren't delivered in the
 * intended order.
 * <P>
 * An I/O subchannel also has an associated buffer pool. Buffers from this pool
 * may be used for read and write events.
 */
public interface IOSubchannel extends Channel {

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
	public EventPipeline responsePipeline();

	/**
	 * Get the subchannel's buffer pool.
	 * 
	 * @return the buffer pool
	 */
	public ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool();
	
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	static String toString(IOSubchannel subchannel) {
		StringBuilder builder = new StringBuilder();
		builder.append(Common.channelToString(subchannel.mainChannel()));
		builder.append("{");
		builder.append(Components.objectName(subchannel));
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Returns the context for the given component. If this is the
	 * first invocation and `createIfAbsent` is `true`, a new context 
	 * will be obtained from the component.
	 * 
	 * @param component the component
	 * @param createIfAbsent create a context if no context exists
	 * @return the context
	 */
	<T> Optional<T> context(
			ContextSupplier<T> component, boolean createIfAbsent);
	
	/**
	 * Returns the context for the given component if it exists.
	 * Equivalent to invoking `context(component, false)`.
	 * 
	 * @param component the component
	 * @return the context
	 */
	default <T> Optional<T> context(ContextSupplier<T> component) {
		return context(component, false);
	}

	/**
	 * Creates a new subchannel of the given component's channel with a new
	 * event pipeline and a buffer pool with two buffers sized 4096.
	 * 
	 * @param component
	 *            the component used to get the main channel and the event
	 *            pipeline
	 * @return the subchannel
	 */
	public static IOSubchannel defaultInstance(Component component) {
		return new DefaultSubchannel(component);
	}

	/**
	 * A simple implementation of {@link IOSubchannel}.
	 */
	public static class DefaultSubchannel implements IOSubchannel {
		private Channel mainChannel;
		private Map<ContextSupplier<?>, ? super Object> 
			contexts = new HashMap<>();
		private EventPipeline responsePipeline;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool;
		
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
		
		/**
		 * Creates a new instance with the main channel
		 * and event pipeline obtained from the component.
		 * 
		 * @param component the manager used to get the main channel
		 * and a new event pipeline
		 */
		public DefaultSubchannel(Manager component) {
			this(component.channel(), component.newEventPipeline());
		}

		protected void setBufferPool(
				ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool) {
			this.bufferPool = bufferPool;
		}
		
		/* (non-Javadoc)
		 * @see org.jgrapes.io.IOSubchannel#getMainChannel()
		 */
		@Override
		public Channel mainChannel() {
			return mainChannel;
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.IOSubchannel#responsePipeline()
		 */
		@Override
		public EventPipeline responsePipeline() {
			return responsePipeline;
		}

		/* (non-Javadoc)
		 * @see org.jgrapes.io.IOSubchannel#context(org.jgrapes.io.ContextSupplier)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public <T> Optional<T> context(
				ContextSupplier<T> component, boolean createIfAbsent) {
			if (!createIfAbsent) {
				return Optional.ofNullable((T)contexts.get(component));
			}
			return Optional.of((T)contexts.computeIfAbsent(
					component, c -> c.createContext()));
		}

		/**
		 * Returns the buffer pool set. If no buffer pool has been set, a
		 * buffer pool with with two buffers of size 4096 is created.
		 */
		public ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool() {
			if (bufferPool == null) {
				bufferPool = new ManagedBufferQueue<>(ManagedByteBuffer.class,
						ByteBuffer.allocate(4096), 
						ByteBuffer.allocate(4096));
			}
			return bufferPool;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return IOSubchannel.toString(this);
		}
		
	}
}
