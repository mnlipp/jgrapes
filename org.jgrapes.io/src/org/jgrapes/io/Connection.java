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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * Represents an I/O connection. I/O connections are bidirectional but
 * asymmetrical. A connection has an initiator that creates and manages
 * the connection and responders that react to the creation of the
 * connection (and other state changes).
 * <P>
 * Events related to a connection are usually handled by two different
 * pipelines. One pipeline, accessible only to the initiator, handles
 * the events from initiator component to responder components. The other, 
 * made available as a property of the connection, handles the events 
 * from responder to initiator. Of course, any pipeline could be used to 
 * send events to the initiator component. However, using arbitrary 
 * pipelines holds the risk that events aren't delivered in the intended 
 * order.
 * 
 * @author Michael N. Lipp
 */
public interface Connection {

	/**
	 * Returns A channel that can be used to fire connection related 
	 * events. This is usually the initiator component's channel. 
	 * Providing this channel as part of the connection simplifies 
	 * the implementation of responder components as they don't 
	 * need any other means to find out where to send response events 
	 * to. Of course, if the initiator component has a well known channel,
	 * this well known channel may be used directly as well.
	 * 
	 * @return the channel
	 */
	Channel getChannel();

	/**
	 * Gets an {@link EventPipeline} that can be used for events going 
	 * back to the initiator on this connection. Consistently using 
	 * this event pipeline for response events ensures that the events 
	 * are written in proper sequence.
	 */
	EventPipeline getResponsePipeline();
	
	/**
	 * Fires the given event on this connection's channel using 
	 * the channel's response pipeline. Effectively, 
	 * {@code respond(someEvent)} is a shortcut for 
	 * {@code getResponsePipeline.add(someEvent, getChannel())}.
	 * 
	 * @param event the event to fire
	 * @return the event (for easy chaining)
	 */
	default <T extends Event<?>> T respond(T event) {
		return getResponsePipeline().add(event, getChannel());
	}
}
