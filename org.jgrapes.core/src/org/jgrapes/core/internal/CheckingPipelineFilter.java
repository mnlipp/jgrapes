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
package org.jgrapes.core.internal;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components.IdInfoProvider;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * A filter that checks the channels parameter when adding events. If no
 * channels are specified, use the channels associated with the event.
 * If there are no channels associated with the event, use the broadcast 
 * channel.
 * 
 * @author Michael N. Lipp
 */
class CheckingPipelineFilter implements EventPipeline, IdInfoProvider {

	private InternalEventPipeline sink;
	private Channel channel;

	/**
	 * Create a new instance that forwards the events to the given
	 * pipeline with the given channel after checking.
	 * 
	 * @param sink
	 */
	public CheckingPipelineFilter(InternalEventPipeline sink, Channel channel) {
		super();
		this.sink = sink;
		this.channel = channel;
	}

	/**
	 * Create a new instance that forwards the events to the given
	 * pipeline after checking.
	 * 
	 * @param sink
	 */
	public CheckingPipelineFilter(InternalEventPipeline sink) {
		this(sink, null);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.EventPipeline#add(org.jgrapes.core.internal.EventBase, org.jgrapes.core.Channel[])
	 */
	@Override
	public <T extends Event<?>> T fire(T event, Channel... channels) {
		if (channels.length == 0) {
			channels = event.getChannels();
			if (channels == null || channels.length == 0) {
				if (channel != null) {
					channels = new Channel[] { channel };
				} else {
					channels = new Channel[] { Channel.BROADCAST };
				}
			}
		}
		event.setChannels(channels);
		return sink.add(event, channels);
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Components.IdInfoProvider#idObject()
	 */
	@Override
	public Object idObject() {
		return sink;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CheckingPipelineFilter [");
		if (sink != null) {
			builder.append("sink=");
			builder.append(sink);
			builder.append(", ");
		}
		if (channel != null) {
			builder.append("channel=");
			builder.append(channel);
		}
		builder.append("]");
		return builder.toString();
	}

}
