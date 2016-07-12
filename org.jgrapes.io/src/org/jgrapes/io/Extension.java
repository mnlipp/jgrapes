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
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.io.util.ManagedByteBuffer;

/**
 * Provides an extension of a channel for sending events downstream
 * from a protocol converter component.
 * 
 * @author Michael N. Lipp
 */
public class Extension implements DataConnection {

	final private Manager converterComponent;
	final private DataConnection upstreamConnection;
	final private EventPipeline responsePipeline;

	/**
	 * 
	 * @param converterComponent
	 * @param upstreamConnection
	 */
	public Extension(Manager converterComponent,
	        DataConnection upstreamConnection) {
		super();
		this.converterComponent = converterComponent;
		this.upstreamConnection = upstreamConnection;
		responsePipeline = converterComponent.newEventPipeline();
	}

	/**
	 * @return the upstreamConnection
	 */
	public DataConnection getUpstreamConnection() {
		return upstreamConnection;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.Connection#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return converterComponent.getChannel();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.Connection#getResponsePipeline()
	 */
	@Override
	public EventPipeline getResponsePipeline() {
		return responsePipeline;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.DataConnection#acquireByteBuffer()
	 */
	@Override
	public ManagedByteBuffer acquireByteBuffer() throws InterruptedException {
		return upstreamConnection.acquireByteBuffer();
	}

}
