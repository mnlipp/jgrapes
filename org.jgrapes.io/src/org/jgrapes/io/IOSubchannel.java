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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
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
 * <P>
 * A subchannel has an initiator that creates and manages the subchannel. Events
 * fired by the initiator are said to flow downstream on the channel. Events
 * fired by components in response are said to flow upstream.
 * <P>
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
 * 
 * @author Michael N. Lipp
 */
public interface IOSubchannel extends Channel {

	/**
	 * Returns the main channel.
	 * 
	 * @return the mainChannel
	 */
	Channel getMainChannel();

	/**
	 * Returns the main channel's match key.
	 * 
	 * @see Channel#getMatchKey()
	 */
	@Override
	default Object getMatchKey() {
		return getMainChannel().getMatchKey();
	}

	/**
	 * Matches the criterion with the main channel.
	 * 
	 * @see Channel#matches(Object)
	 */
	@Override
	default boolean matches(Object criterion) {
		return getMainChannel().matches(criterion);
	}

	/**
	 * Gets the {@link EventPipeline} that can be used for events going back to
	 * the initiator of this connection. Consistently using this event pipeline
	 * for response events ensures that the events are written in proper
	 * sequence.
	 */
	public abstract EventPipeline getResponsePipeline();

	/**
	 * Get the subchannel's buffer pool.
	 * 
	 * @return the buffer pool
	 */
	public abstract 
		ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool();
	
	/**
	 * Fires the given event on this subchannel using the subchannel's response
	 * pipeline. Effectively, {@code fire(someEvent)} is a shortcut for
	 * {@code getResponsePipeline.add(someEvent, this)}.
	 * 
	 * @param event
	 *            the event to fire
	 * @return the event (for easy chaining)
	 */
	default <T extends Event<?>> T fire(T event) {
		return getResponsePipeline().fire(event, this);
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
	
	public static class DefaultSubchannel implements IOSubchannel {

		private Channel mainChannel;
		private EventPipeline eventPipeline;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool;

		private DefaultSubchannel(Component component) {
			mainChannel = component.getChannel();
			eventPipeline = component.newEventPipeline();
		}
		
		/* (non-Javadoc)
		 * @see org.jgrapes.io.IOSubchannel#getMainChannel()
		 */
		@Override
		public Channel getMainChannel() {
			return mainChannel;
		}

		@Override
		public EventPipeline getResponsePipeline() {
			return eventPipeline;
		}

		@Override
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(Components.objectName(this));
			builder.append("(");
				builder.append(Common.channelToString(mainChannel));
			builder.append(")");
			return builder.toString();
		}

	}
}
