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

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.internal.Common;
import org.jgrapes.io.IOSubchannel;

/**
 * Provides an I/O subchannel that is linked to another I/O subchannel. A
 * typical use case for this class is a protocol converter.
 * <P>
 * Protocol converters receive events related to an I/O resource from upstream,
 * and while processing them usually generate new events to other components
 * downstream (and vice versa). The events received are associated with a
 * particular resource by the subchannel that is used to relay them. The
 * association with the resource must be maintained for the newly generated
 * events as well. It is, however, not possible to use the same subchannel for
 * receiving from upstream and sending downstream because it wouldn't be
 * possible to distinguish between e.g. an {@code Input} event from upstream to
 * the converter and an {@code Input} event (conveying the converted data) from
 * the converter to the downstream components.
 * <P>
 * Therefore, the converter must provide and manage independent subchannels for
 * the data streams on its downstream side with a one-to-one relationship to the
 * upstream subchannels. The {@code LinkedIOSubchannel} class simplifies this
 * task. It provides a new subchannel with its own pipeline and a reference to
 * an existing subchannel. This makes it easy to find the upstream subchannel
 * for a given downstream ({@code LinkedIOSubchannel}) when processing response
 * events. For finding the downstream {@code IOSubchannel} for a given upstream
 * connection, the class maintains a mapping in a {@link WeakHashMap}.
 * <P>
 * Note that memory management is a bit special here. The 
 * {@code LinkedIOSubchannel} should exist as long as the subchannel that
 * it is linked to exists. This subchannel, however, does not have a reference
 * to the {@code LinkedIOSubchannel}. Unless other references exist, the
 * existence of the {@code LinkedIOSubchannel} is ensured only by the reference
 * as value in the {@code WeakHashMap}. This reference will go away when
 * the upstream subchannel (used as key) goes away.
 * 
 * @author Michael N. Lipp
 */
public class LinkedIOSubchannel implements IOSubchannel {

	private static final Map<IOSubchannel, LinkedIOSubchannel> 
		reverseMap = Collections.synchronizedMap(new WeakHashMap<>());

	private final Manager converterComponent;
	// Must be weak, else there will always be a reference to the 
	// upstream channel and, through the reverseMap, to this object.
	private final WeakReference<IOSubchannel> upstreamChannel;
	private final EventPipeline responsePipeline;

	/**
	 * Creates a new {@code LinkedIOSubchannel} that links to the give I/O
	 * subchannel. Using this constructor is similar to invoking
	 * {@link #LinkedIOSubchannel(Manager, IOSubchannel, boolean)} with
	 * {@code true} as last parameter.
	 * 
	 * @param converterComponent
	 *            the converter component; used to get the main channel and the
	 *            new event pipeline
	 * @param upstreamChannel
	 *            the upstream channel
	 */
	public LinkedIOSubchannel(Manager converterComponent,
	        IOSubchannel upstreamChannel) {
		this(converterComponent, upstreamChannel, true);
	}

	/**
	 * Creates a new {@code LinkedIOSubchannel} for a given I/O subchannel.
	 * Using this constructor with {@code false} as last parameter prevents the
	 * addition of the linked I/O subchannel to the mapping from (upstream)
	 * subchannel to linked subchannel. This can save some space if a converter
	 * component has some other means to maintain that information. Addition to
	 * the map is thread safe.
	 * 
	 * @param converterComponent
	 *            the converter component; used to get the main channel and the
	 *            new event pipeline
	 * @param upstreamChannel
	 *            the upstream channel
	 * @param addToMap
	 *            add an entry in the map
	 */
	public LinkedIOSubchannel(Manager converterComponent,
	        IOSubchannel upstreamChannel, boolean addToMap) {
		super();
		this.converterComponent = converterComponent;
		this.upstreamChannel = new WeakReference<>(upstreamChannel);
		responsePipeline = converterComponent.newEventPipeline();
		if (addToMap) {
			reverseMap.put(upstreamChannel, this);
		}
	}

	/**
	 * @return the converterComponent
	 */
	public Manager converterComponent() {
		return converterComponent;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.IOSubchannel#getMainChannel()
	 */
	@Override
	public Channel mainChannel() {
		return converterComponent.channel();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.io.IOSubchannel#getResponsePipeline()
	 */
	@Override
	public EventPipeline responsePipeline() {
		return responsePipeline;
	}

	/**
	 * @return the upstream channel
	 */
	public IOSubchannel upstreamChannel() {
		return upstreamChannel.get();
	}

	/**
	 * Delegates the invocation to the upstream channel.
	 * 
	 * @see org.jgrapes.io.IOSubchannel#bufferPool()
	 */
	@Override
	public ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> bufferPool() {
		IOSubchannel up = upstreamChannel.get();
		return up == null ? null : up.bufferPool();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Components.objectName(this));
		builder.append("(");
			builder.append(Common.channelToString(mainChannel()));
		builder.append(")");
		builder.append(" [");
		if (upstreamChannel != null) {
			builder.append("―>");
			builder.append(Common.channelToString(upstreamChannel.get()));
		}
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Returns the linked subchannel that has been created for the given
	 * subchannel. If more than one linked subchannel has been created for a
	 * subchannel, the linked subchannel created last is returned.
	 * 
	 * @param channel
	 *            the channel
	 * @return the linked subchannel created for the given subchannel or
	 *         {@code null} if no such linked subchannel exists
	 */
	public static LinkedIOSubchannel lookupLinked(IOSubchannel channel) {
		return reverseMap.get(channel);
	}
}
