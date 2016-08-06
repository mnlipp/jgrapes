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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.io.Connection;

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
 * <P>
 * For finding the downstream connection ({@code Extension}) for a given
 * upstream connection, the class maintains a mapping in a {@link WeakHashMap}.
 * 
 * @author Michael N. Lipp
 */
public class Extension implements Connection {

	final private static Map<Connection, Extension> reverseMap = Collections
	        .synchronizedMap(new WeakHashMap<>());

	final private Manager converterComponent;
	final private Connection upstreamConnection;
	final private EventPipeline responsePipeline;

	/**
	 * Creates a new {@code Extension} for a given connection. Using this
	 * constructor is similar to invoking
	 * {@link #Extension(Manager, DataConnection, boolean)} with {@code true} as
	 * last parameter.
	 * 
	 * @param converterComponent
	 *            the converter component; its channel is returned by
	 *            {@link #getChannel()}
	 * @param upstreamConnection
	 *            the upstream connection
	 */
	public Extension(Manager converterComponent,
	        Connection upstreamConnection) {
		this(converterComponent, upstreamConnection, true);
	}

	/**
	 * Creates a new {@code Extension} for a given connection. Using this
	 * constructor with {@code false} as last parameter prevents the addition of
	 * the extension to the mapping from connection to extension. This can save
	 * some space if converter component has some other means to maintain that
	 * information. Addition to the map is thread safe.
	 * 
	 * @param converterComponent
	 *            the component; its channel is returned by
	 *            {@link #getChannel()}
	 * @param upstreamConnection
	 *            the upstream connection
	 * @param addToMap
	 *            add an entry in the map
	 */
	public Extension(Manager converterComponent,
	        Connection upstreamConnection, boolean addToMap) {
		super();
		this.converterComponent = converterComponent;
		this.upstreamConnection = upstreamConnection;
		responsePipeline = converterComponent.newEventPipeline();
		if (addToMap) {
			reverseMap.put(upstreamConnection, this);
		}
	}

	/**
	 * @return the upstreamConnection
	 */
	public Connection getUpstreamConnection() {
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

	/**
	 * Delegates the invocation to the upstream connection.
	 * 
	 * @see org.jgrapes.io.Connection#bufferPool()
	 */
	@Override
	public ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool() {
		return upstreamConnection.bufferPool();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append(" [");
		if (upstreamConnection != null) {
			builder.append("upstreamConnection=");
			builder.append(Components.objectName(upstreamConnection));
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Returns the extension that has been created for the given connection. If
	 * more than one extension has been created for a connection, the extension
	 * created last is returned.
	 * <P>
	 * Having more than one extension for a given connection is a very unusual
	 * use case. Another mechanism must be used for the reverse mapping in
	 * that case.
	 * 
	 * @param connection the connection
	 * @return the extension created for this extension or {@code null}
	 * if no such extension exists
	 */
	public static Connection lookupExtension(Connection connection) {
		return reverseMap.get(connection);
	}
}
