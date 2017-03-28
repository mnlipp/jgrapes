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

package org.jgrapes.io.events;

import java.nio.channels.SelectableChannel;

import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletedEvent;
import org.jgrapes.core.Event;
import org.jgrapes.io.NioHandler;

/**
 * @author Michael N. Lipp
 */
public class NioRegistration extends Event<NioRegistration.Registration> {

	public abstract static class Registration {
		public abstract void updateInterested(int ops);
	}
	
	public static class Completed 
		extends CompletedEvent<NioRegistration> {

		public Completed(Channel... channels) {
			super(channels);
		}
		
	}
	
	private NioHandler handler;
	private SelectableChannel ioChannel;
	private int ops;

	/**
	 * Creates a new registration event for the given handler, using the given
	 * NIO channel and handling the given operations. The completed event
	 * for this event is to be sent to the given channel. 
	 * 
	 * @param handler the handler
	 * @param ioChannel the NIO channel
	 * @param ops the supported operations
	 * @param completedTarget where to send the completed event to
	 */
	public NioRegistration(NioHandler handler, SelectableChannel ioChannel,
	        int ops, Channel completedTarget) {
		super(new Completed(completedTarget));
		this.handler = handler;
		this.ioChannel = ioChannel;
		this.ops = ops;
	}

	/**
	 * @return the handler
	 */
	public NioHandler handler() {
		return handler;
	}

	/**
	 * @return the channel
	 */
	public SelectableChannel ioChannel() {
		return ioChannel;
	}

	/**
	 * @return the ops
	 */
	public int ops() {
		return ops;
	}

}
