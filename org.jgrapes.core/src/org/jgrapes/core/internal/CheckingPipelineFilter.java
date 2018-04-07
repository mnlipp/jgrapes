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

package org.jgrapes.core.internal;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.IdInfoProvider;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * A filter that checks the channels parameter when adding events. If no
 * channels are specified, use the channel from the constructor. If this
 * channel is `null`, use the channels associated with the event.
 * If there are no channels associated with the event, use the broadcast 
 * channel.
 */
class CheckingPipelineFilter
        implements EventPipeline, InternalEventPipelineWrapper, IdInfoProvider {

    private InternalEventPipeline sink;
    private Channel channel;
    private WeakReference<InternalEventPipelineWrapper> allowedSourceRef;
    private final ThreadLocal<Boolean> allowNext = new ThreadLocal<>();

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

    @Override
    public InternalEventPipeline wrapped() {
        return sink;
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

    @Override
    public EventPipeline restrictEventSource(EventPipeline sourcePipeline) {
        allowedSourceRef = sourcePipeline == null ? null
            : new WeakReference<>(
                (InternalEventPipelineWrapper) sourcePipeline);
        return this;
    }

    @Override
    public EventPipeline overrideRestriction() {
        allowNext.set(true);
        return this;
    }

    @Override
    @SuppressWarnings("PMD.GuardLogStatement")
    public <T extends Event<?>> T fire(T event, Channel... channels) {
        if (allowedSourceRef != null) {
            boolean allowed = allowNext.get() != null && allowNext.get();
            allowNext.set(null);
            if (!allowed // i.e. if not allowed anyway...
                && (allowedSourceRef.get() == null
                    || ((InternalEventPipelineWrapper) allowedSourceRef.get())
                        .wrapped() != FeedBackPipelineFilter
                            .getAssociatedPipeline())) {
                Common.fireRestrictionLogger.log(Level.SEVERE,
                    Components.objectName(FeedBackPipelineFilter
                        .getAssociatedPipeline()) + " cannot add "
                        + event.toString() + " to pipeline "
                        + Components.objectName(this.wrapped())
                        + " (accepts only from "
                        + Components
                            .objectName(allowedSourceRef.get().wrapped())
                        + ").",
                    new IllegalArgumentException());
                return event;
            }
        }
        if (channels.length == 0) {
            channels = event.channels();
            if (channels.length == 0) {
                if (channel == null) {
                    channels = new Channel[] { Channel.BROADCAST };
                } else {
                    channels = new Channel[] { channel };
                }
            }
        }
        event.setChannels(channels);
        return sink.add(event, channels);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.EventPipeline#submit(java.util.concurrent.Callable)
     */
    @Override
    public <V> Future<V> submit(Callable<V> action) {
        ActionEvent<V> event = ActionEvent.create(action);
        return sink.add(event, Channel.BROADCAST);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.EventPipeline#submit(java.lang.Runnable)
     */
    @Override
    public void submit(Runnable action) {
        ActionEvent<Void> event = ActionEvent.create(action);
        sink.add(event, Channel.BROADCAST);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.Components.IdInfoProvider#idObject()
     */
    @Override
    public Object idObject() {
        return sink;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.EventPipeline#executorService()
     */
    @Override
    public ExecutorService executorService() {
        return sink.executorService();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
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
        builder.append(']');
        return builder.toString();
    }

}
