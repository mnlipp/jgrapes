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

import java.util.concurrent.ExecutorService;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * This class provides the default implementation of an {@link EventPipeline}.
 */
public class EventProcessor implements InternalEventPipeline, Runnable {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final ThreadLocal<EventBase<?>> newEventsParent
        = new ThreadLocal<>();

    private final ExecutorService executorService;
    private final ComponentTree componentTree;
    private final EventPipeline asEventPipeline;
    protected final EventQueue queue = new EventQueue();
    private boolean isExecuting;

    /**
     * Instantiates a new event processor.
     *
     * @param tree the tree
     */
    /* default */ EventProcessor(ComponentTree tree) {
        this(tree, Components.defaultExecutorService());
    }

    /* default */ EventProcessor(ComponentTree tree,
            ExecutorService executorService) {
        this.componentTree = tree;
        this.executorService = executorService;
        asEventPipeline = new CheckingPipelineFilter(tree, this);
    }

    /**
     * Gets the component tree.
     *
     * @return the component tree
     */
    protected ComponentTree tree() {
        return componentTree;
    }

    /* default */ EventPipeline asEventPipeline() {
        return asEventPipeline;
    }

    /**
     * Called before adding completion events. The parent of
     * a completion event is not the event that has completed but
     * the event that generated the original event.
     *
     * @param parent the new parent
     */
    /* default */ void updateNewEventsParent(EventBase<?> parent) {
        newEventsParent.set(parent);
    }

    @Override
    public <T extends Event<?>> T add(T event, Channel... channels) {
        ((EventBase<?>) event).generatedBy(newEventsParent.get());
        ((EventBase<?>) event).processedBy(this);
        queue.add(event, channels);
        synchronized (this) {
            if (!isExecuting) {
                GeneratorRegistry.instance().add(this);
                isExecuting = true;
                executorService.execute(this);
            }
        }
        return event;
    }

    /* default */ void add(EventQueue source) {
        while (true) {
            EventChannelsTuple entry = source.poll();
            if (entry == null) {
                break;
            }
            entry.event.processedBy(this);
            queue.add(entry);
        }
        synchronized (this) {
            if (!isExecuting) {
                GeneratorRegistry.instance().add(this);
                isExecuting = true;
                executorService.execute(this);
            }
        }
    }

    @Override
    public void merge(InternalEventPipeline other) {
        if (!(other instanceof BufferingEventPipeline)) {
            throw new IllegalArgumentException(
                "Can only merge events from an BufferingEventPipeline.");
        }
        add(((BufferingEventPipeline) other).retrieveEvents());
    }

    @Override
    public void run() {
        String origName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(
                origName + " (P" + Components.objectId(this) + ")");
            componentTree.setDispatchingPipeline(this);
            while (true) {
                // No lock needed if queue is filled
                EventChannelsTuple next = queue.peek();
                if (next == null) {
                    synchronized (this) {
                        // Retry with lock for proper synchronization
                        next = queue.peek();
                        if (next == null) {
                            GeneratorRegistry.instance().remove(this);
                            isExecuting = false;
                            break;
                        }
                    }
                }
                newEventsParent.set(next.event);
                componentTree.dispatch(
                    asEventPipeline, next.event, next.channels);
                newEventsParent.get().decrementOpen();
                queue.remove();
            }
        } finally {
            newEventsParent.set(null);
            componentTree.setDispatchingPipeline(null);
            Thread.currentThread().setName(origName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.internal.InternalEventPipeline#executorService()
     */
    @Override
    public ExecutorService executorService() {
        return executorService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Components.objectName(this))
            .append(" [");
        if (queue != null) {
            builder.append("queue=")
                .append(queue);
        }
        builder.append(']');
        return builder.toString();
    }

}
