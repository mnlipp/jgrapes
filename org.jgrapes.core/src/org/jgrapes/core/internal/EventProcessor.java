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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * This class provides the default implementation of an {@link EventPipeline}.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class EventProcessor implements InternalEventPipeline, Runnable {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final ThreadLocal<EventBase<?>> newEventsParent
        = new ThreadLocal<>();

    private final ExecutorService executorService;
    private final ComponentTree componentTree;
    private final EventPipeline asEventPipeline;
    // Must not use synchronized in toString, leads to unexpected deadlock
    protected final Queue<EventChannelsTuple> queue
        = new ConcurrentLinkedQueue<>();
    private Iterator<HandlerReference> invoking;
    // Used by this thread only.
    private final Set<EventBase<?>> suspended = new HashSet<>();
    // Only this thread can remove, but others might add.
    private final Queue<EventBase<?>> toBeResumed
        = new ConcurrentLinkedDeque<>();
    private boolean isExecuting;
    private final ThreadLocal<Thread> executor = new ThreadLocal<>();

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
        synchronized (this) {
            EventChannelsTuple.addTo(queue, event, channels);
            if (!isExecuting) {
                // Queue was initially empty, this starts it
                GeneratorRegistry.instance().add(this);
                isExecuting = true;
                executorService.execute(this);
            }
        }
        return event;
    }

    @SuppressWarnings("PMD.ConfusingTernary")
    /* default */ void add(Queue<EventChannelsTuple> source) {
        synchronized (this) {
            while (true) {
                EventChannelsTuple entry = source.poll();
                if (entry == null) {
                    break;
                }
                entry.event.processedBy(this);
                queue.add(entry);
            }
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
    @SuppressWarnings({ "PMD.AvoidDeeplyNestedIfStmts",
        "PMD.CognitiveComplexity" })
    public void run() {
        String origName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(
                origName + " (P" + Components.objectId(this) + ")");
            executor.set(Thread.currentThread());
            componentTree.setDispatchingPipeline(this);
            while (true) {
                // No lock needed, only this thread can remove from resumed
                var resumedEvent = toBeResumed.poll();
                if (resumedEvent != null) {
                    if (suspended.remove(resumedEvent)) {
                        resumedEvent.invokeWhenResumed();
                        invokeHandlers(resumedEvent.clearSuspendedHandlers(),
                            resumedEvent);
                    }
                    continue;
                }

                EventChannelsTuple next;
                synchronized (this) {
                    next = queue.poll();
                    if (next == null) {
                        // Everything is done, though suspended handlers
                        // may cause this processor to be reactivated.
                        GeneratorRegistry.instance().remove(this);
                        isExecuting = false;
                        synchronized (executor) {
                            executor.notifyAll();
                        }
                        break;
                    }
                }
                HandlerList handlers
                    = componentTree.getEventHandlers(next.event, next.channels);
                invokeHandlers(handlers.iterator(), next.event);
            }
        } finally {
            // This processor should now only be (strongly) referenced
            // from suspended events (if any exist), the
            // CheckingPipelineFilter (which is only referenced from this)
            // and some component tree, if this is the tree's default
            // processor.
            newEventsParent.set(null);
            componentTree.setDispatchingPipeline(null);
            executor.set(null);
            Thread.currentThread().setName(origName);
        }
    }

    /**
     * Invoke all (remaining) handlers with the given event as parameter.
     *
     * @param handlers the handlers
     * @param event the event
     */
    private void invokeHandlers(Iterator<HandlerReference> handlers,
            EventBase<?> event) {
        try {
            invoking = handlers;
            newEventsParent.set(event);
            // invoking may be set to null by suspendHandling()
            while (invoking != null && invoking.hasNext()) {
                HandlerReference hdlr = invoking.next();
                try {
                    if (event.isStopped()) {
                        break;
                    }
                    hdlr.invoke(event);
                } catch (AssertionError t) {
                    // JUnit support
                    CoreUtils.setAssertionError(t);
                    event.handlingError(asEventPipeline, t);
                } catch (Error e) { // NOPMD
                    // Wouldn't have caught it, if it was possible.
                    throw e;
                } catch (Throwable t) { // NOPMD
                    // Errors have been rethrown, so this should work.
                    event.handlingError(asEventPipeline, t);
                }
            }
        } catch (AssertionError t) {
            // JUnit support
            CoreUtils.setAssertionError(t);
            event.handlingError(asEventPipeline, t);
        } catch (Error e) { // NOPMD
            // Wouldn't have caught it, if it was possible.
            throw e;
        } catch (Throwable t) { // NOPMD
            // Errors have been rethrown, so this should work.
            event.handlingError(asEventPipeline, t);
        } finally { // NOPMD
            if (invoking != null) {
                event.handled();
                invoking = null;
                newEventsParent.get().decrementOpen();
            }
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    /* default */ void suspendHandling(EventBase<?> event) {
        if (Thread.currentThread() != executor.get()) {
            throw new IllegalStateException("May only be called from handler.");
        }
        if (!invoking.hasNext()) {
            // Last anyway, nothing to be done
            return;
        }
        event.setSuspendedHandlers(invoking);
        invoking = null;
        suspended.add(event);
        // Just in case (might happen)
        toBeResumed.remove(event);
    }

    /* default */ void resumeHandling(EventBase<?> event) {
        toBeResumed.add(event);
        synchronized (this) {
            if (!isExecuting) {
                // There were no more events, restart
                GeneratorRegistry.instance().add(this);
                isExecuting = true;
                executorService.execute(this);
            }
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

    @Override
    public void awaitExhaustion() throws InterruptedException {
        synchronized (executor) {
            while (isExecuting) {
                executor.wait();
            }
        }
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
            builder.append("queue=").append(queue);
        }
        builder.append(']');
        return builder.toString();
    }

}
