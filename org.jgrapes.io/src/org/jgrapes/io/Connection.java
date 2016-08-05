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
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * Represents an I/O connection. I/O connections are asymmetrical. A connection
 * has an initiator that creates and manages the connection and responders that
 * reacts to the creation of the connection (and other state changes).
 * Responders can also send an event to close the connection.
 * <P>
 * Events related to a connection are usually handled by two different pipelines
 * managed by the initiator. One pipeline, accessible only to the initiator,
 * handles the events from the initiator component to responder components. The
 * other, made available as a property of the connection, handles the events
 * from responder to initiator. Of course, any pipeline could be used to send
 * events to the initiator component. However, using arbitrary pipelines holds
 * the risk that events aren't delivered in the intended order.
 * <P>
 * A connection has an associated buffer pool. Buffers from this pool may be
 * used for read and write events.
 * 
 * @author Michael N. Lipp
 */
public interface Connection {

	/**
	 * Returns A channel that can be used to fire connection related events.
	 * This is usually the initiator component's channel. Providing this channel
	 * as part of the connection simplifies the implementation of responder
	 * components as they don't need any other means to find out where to send
	 * response events to. Of course, if the initiator component has a well
	 * known channel, this well known channel can be used directly.
	 * 
	 * @return the channel
	 */
	Channel getChannel();

	/**
	 * Gets the {@link EventPipeline} that can be used for events going back to
	 * the initiator of this connection. Consistently using this event pipeline
	 * for response events ensures that the events are written in proper
	 * sequence.
	 */
	EventPipeline getResponsePipeline();

	/**
	 * Get the connection's buffer pool.
	 * 
	 * @return the buffer pool
	 */
	ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool();
	
	/**
	 * Fires the given event on this connection's channel using the channel's
	 * response pipeline. Effectively, {@code respond(someEvent)} is a shortcut
	 * for {@code getResponsePipeline.add(someEvent, getChannel())}.
	 * 
	 * @param event
	 *            the event to fire
	 * @return the event (for easy chaining)
	 */
	default <T extends Event<?>> T respond(T event) {
		return getResponsePipeline().fire(event, getChannel());
	}

	/**
	 * Creates a new connection with the channel set to the given component's
	 * channel and a new event pipeline.
	 * 
	 * @param component
	 *            the component used to get the channel and the event pipeline
	 * @return the connection
	 */
	static Connection newConnection(Component component) {
		return new DefaultConnection(component);
	}
	
	public static class DefaultConnection implements Connection {

		private Channel channel;
		private EventPipeline eventPipeline;
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool;

		private DefaultConnection(Component component) {
			channel = component.getChannel();
			eventPipeline = component.newEventPipeline();
		}
		
		@Override
		public Channel getChannel() {
			return channel;
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
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(Components.objectName(this, Connection.class));
			builder.append(" [");
			if (channel != null) {
				builder.append("channel=");
				builder.append(Components.objectName(channel));
				builder.append(", ");
			}
			if (eventPipeline != null) {
				builder.append("eventPipeline=");
				builder.append(Components.objectName(eventPipeline));
			}
			builder.append("]");
			return builder.toString();
		}

	}
}
