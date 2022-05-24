/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.internal;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.events.Start;

/**
 * The buffering event pipeline is used before a tree has been started. 
 * It simply buffers all events until a {@link Start} event is added.
 */
public class BufferingEventPipeline implements InternalEventPipeline {

    private final ComponentTree componentTree;
    /** Buffered events. */
    private Queue<EventChannelsTuple> buffered = new LinkedList<>();
    /** The event pipeline that we delegate to after the start
     * event has been detected. */
    private InternalEventPipeline activePipeline;

    /**
     * Instantiates a new buffering event pipeline.
     *
     * @param componentTree the component tree
     */
    /* default */ BufferingEventPipeline(ComponentTree componentTree) {
        super();
        this.componentTree = componentTree;
    }

    @Override
    public void merge(InternalEventPipeline other) {
        synchronized (this) {
            if (!(other instanceof BufferingEventPipeline)) {
                throw new IllegalArgumentException(
                    "Can only merge events from an BufferingEventPipeline.");
            }
            buffered.addAll(((BufferingEventPipeline) other).retrieveEvents());
        }
    }

    @Override
    public <T extends Event<?>> T add(T event, Channel... channels) {
        synchronized (this) {
            // If thread1 adds the start event and thread2 gets here before
            // thread1 has changed the event processor for the tree, send the
            // event to the event processor that should already have been used.
            if (activePipeline != null) {
                activePipeline.add(event, channels);
                return event;
            }
            // Invoke although argument is null!
            ((EventBase<?>) event).generatedBy(null);
            EventChannelsTuple.addTo(buffered, event, channels);
            if (event instanceof Start) {
                // Merge all events into a "standard" event processor
                // and set it as default processor for the tree (with
                // any thread specific pipelines taking precedence).
                EventProcessor processor = new EventProcessor(componentTree);
                activePipeline
                    = new FeedBackPipelineFilter(componentTree, processor);
                componentTree.setEventPipeline(activePipeline);
                processor.add(buffered);
            }
            return event;
        }
    }

    /* default */ Queue<EventChannelsTuple> retrieveEvents() {
        synchronized (this) {
            Queue<EventChannelsTuple> old = buffered;
            buffered = new ConcurrentLinkedDeque<>();
            return old;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.internal.InternalEventPipeline#executorService()
     */
    @Override
    public ExecutorService executorService() {
        return Components.defaultExecutorService();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append("BufferingEventPipeline [");
        // Avoid problem with concurrency
        var bufd = buffered;
        if (bufd != null) {
            builder.append("buffered=");
            builder.append(bufd);
        }
        builder.append(']');
        return builder.toString();
    }
}
