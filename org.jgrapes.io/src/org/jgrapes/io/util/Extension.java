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
package org.jgrapes.io.util;

import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.io.Connection;
import org.jgrapes.io.DataConnection;

/**
 * Provides an extension of a connection for sending events downstream from a
 * protocol converter component.
 * <P>
 * Protocol converters receive data related to a connection in some format from
 * upstream, process it and forward it to other components downstream (and vice
 * versa). This implies that there is a one-to-one relationship between the
 * upstream and the downstream connection. It is, however, not possible to use
 * the same connection on the upstream and on the downstream side.
 * <P>
 * Aside from identifying a logical connection, {@link Connection} objects
 * provide an associated channel and an associated pipeline for firing events.
 * While it would be possible to share the pipeline for downstream and upstream
 * events, sharing the channel doesn't make sense, because events between the
 * converter and the upstream component must be fired on a different channel
 * than the events between the converter and the downstream component. Else it
 * wouldn't be possible to distinguish between e.g. a {@code Write} event from
 * upstream to the converter and a {@code Write} event from the converter to the
 * downstream components.
 * <P>
 * Therefore, the converter must provide and manage its own connections for the
 * data streams on the downstream side. The {@code Extension} class simplifies
 * this task. It provides a new connection with its own channel and pipeline and
 * a reference to an existing connection. This makes it easy to find the
 * upstream connection for a given downstream ({@code Extension}) connection.
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jgrapes.io.Connection#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return converterComponent.getChannel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jgrapes.io.Connection#getResponsePipeline()
	 */
	@Override
	public EventPipeline getResponsePipeline() {
		return responsePipeline;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jgrapes.io.DataConnection#acquireByteBuffer()
	 */
	@Override
	public ManagedByteBuffer acquireByteBuffer() throws InterruptedException {
		return upstreamConnection.acquireByteBuffer();
	}

}
