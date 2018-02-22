/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
import java.util.Optional;

import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultSubchannel;

/**
 * Provides an I/O subchannel that is linked to another I/O subchannel. A
 * typical use case for this class is a protocol converter.
 * 
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
 * 
 * Therefore, the converter must provide and manage independent subchannels for
 * the data streams on its downstream side with a one-to-one relationship to the
 * upstream subchannels. The {@code LinkedIOSubchannel} class simplifies this
 * task. It provides a new subchannel with a reference to an existing 
 * subchannel. This makes it easy to find the upstream subchannel for a 
 * given downstream ({@code LinkedIOSubchannel}) when processing response
 * events. For finding the downstream {@code IOSubchannel} for a given upstream
 * connection, instances associate themselves with the upstream channel using
 * the converter component as key. This allows a subchannel to have several
 * associated linked subchannels.
 */
public class LinkedIOSubchannel extends DefaultSubchannel {

	private final Manager hub;
	// Must be weak, else there will always be a reference to the 
	// upstream channel and, through the reverseMap, to this object.
	private final WeakReference<IOSubchannel> upstreamChannel;
	
	/**
	 * Creates a new {@code LinkedIOSubchannel} for a given main channel
	 * that links to the give I/O subchannel. Using this constructor 
	 * is similar to invoking
	 * {@link #LinkedIOSubchannel(Manager, Channel, IOSubchannel, EventPipeline, boolean)}
	 * with {@code true} as last parameter.
	 *
	 * @param hub the component that manages this channel
	 * @param mainChannel the main channel
	 * @param upstreamChannel the upstream channel
	 * @param responsePipeline the response pipeline
	 */
	public LinkedIOSubchannel(Manager hub, Channel mainChannel, 
	        IOSubchannel upstreamChannel, EventPipeline responsePipeline) {
		this(hub, mainChannel, upstreamChannel, responsePipeline, true);
	}

	/**
	 * Creates a new {@code LinkedIOSubchannel} for a given main channel
	 * that links to a given I/O subchannel.
	 * 
	 * Using this constructor with {@code false} as last parameter prevents the
	 * addition of the back link from the upstream channel to the downstream
	 * channel (see {@link #downstreamChannel(Manager, IOSubchannel)}). 
	 * This can save some space if a converter component has some other 
	 * means to maintain that information. Addition to
	 * the map is thread safe.
	 *
	 * @param hub the converter component that manages this channel
	 * @param mainChannel the main channel
	 * @param upstreamChannel the upstream channel
	 * @param responsePipeline the response pipeline
	 * @param linkBack create the link from upstream to downstream
	 */
	public LinkedIOSubchannel(Manager hub, Channel mainChannel,
	        IOSubchannel upstreamChannel, EventPipeline responsePipeline,
	        boolean linkBack) {
		super(mainChannel, responsePipeline);
		this.hub = hub;
		this.upstreamChannel = new WeakReference<>(upstreamChannel);
		if (linkBack) {
			upstreamChannel.setAssociated(
					new KeyWrapper(hub), this);
		}
	}

	/**
	 * @return the component that manages this channel
	 */
	public Manager hub() {
		return hub;
	}

	/**
	 * @return the upstream channel
	 */
	public IOSubchannel upstreamChannel() {
		return upstreamChannel.get();
	}
	
	/**
	 * Delegates the invocation to the upstream channel 
	 * if no associated data is found for this channel. 
	 */
	@Override
	public <V> Optional<V> associated(Object by, Class<V> type) {
		Optional<V> result = super.associated(by, type);
		if (!result.isPresent()) {
			IOSubchannel up = upstreamChannel();
			if (up != null) {
				return up.associated(by, type);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(IOSubchannel.toString(this));
		builder.append(" (");
		if (upstreamChannel != null) {
			builder.append("―>");
			builder.append(Channel.toString(upstreamChannel.get()));
		}
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Returns the linked downstream channel that has been created for the 
	 * given component and (upstream) subchannel. If more than one linked 
	 * subchannel has been created for a given component and subchannel, 
	 * the linked subchannel created last is returned.
	 *
	 * @param hub the component that manages this channel
	 * @param upstreamChannel the (upstream) channel
	 * @return the linked downstream subchannel created for the
	 * given component and (upstream) subchannel if it exists
	 */
	public static Optional<? extends LinkedIOSubchannel> downstreamChannel(
			Manager hub, IOSubchannel upstreamChannel) {
		return upstreamChannel.associated(
				new KeyWrapper(hub), LinkedIOSubchannel.class);
	}
	
	private static class KeyWrapper {

		private Manager hub;

		/**
		 * @param hub
		 */
		public KeyWrapper(Manager hub) {
			super();
			this.hub = hub;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((hub == null) ? 0
			        : hub.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			KeyWrapper other = (KeyWrapper) obj;
			if (hub == null) {
				if (other.hub != null) {
					return false;
				}
			} else if (!hub.equals(other.hub)) {
				return false;
			}
			return true;
		}
	}
}
