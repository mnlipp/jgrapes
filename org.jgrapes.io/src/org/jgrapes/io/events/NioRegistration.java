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
import org.jgrapes.core.Event;
import org.jgrapes.core.events.AbstractCompletedEvent;
import org.jgrapes.io.NioHandler;

/**
 * @author Michael N. Lipp
 */
public class NioRegistration extends Event<NioRegistration.Registration> {

	static public abstract class Registration {
		public abstract void updateInterested(int ops);
	}
	
	public class Completed extends AbstractCompletedEvent<NioRegistration> {

		/**
		 * @param initialEvent
		 */
		public Completed(NioRegistration initialEvent, Channel target) {
			super(initialEvent);
			setChannels(target);
		}
	}
	
	private NioHandler handler;
	private SelectableChannel ioChannel;
	private int ops;
	
	/**
	 * @param handler
	 * @param channel
	 * @param ops
	 */
	public NioRegistration(NioHandler handler, SelectableChannel ioChannel,
	        int ops, Channel completedTarget) {
		super();
		this.handler = handler;
		this.ioChannel = ioChannel;
		this.ops = ops;
		setCompletedEvent(new Completed(this, completedTarget));
	}

	/**
	 * @return the handler
	 */
	public NioHandler getHandler() {
		return handler;
	}

	/**
	 * @return the channel
	 */
	public SelectableChannel getIoChannel() {
		return ioChannel;
	}

	/**
	 * @return the ops
	 */
	public int getOps() {
		return ops;
	}

}
