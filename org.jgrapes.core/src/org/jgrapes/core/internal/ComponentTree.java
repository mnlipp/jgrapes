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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;

/**
 * This class represents the component tree. It holds all properties that 
 * are common to all nodes of a component tree (the {@link ComponentVertex}s).
 */
class ComponentTree {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger handlerTracking
        = Logger.getLogger(ComponentType.class.getPackage().getName()
            + ".handlerTracking");

    private final ComponentVertex root;
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Used synchronized only
    private final Map<CacheKey, HandlerList> handlerCache = new HashMap<>();
    private InternalEventPipeline eventPipeline;
    private static HandlerReference fallbackErrorHandler;
    private static HandlerReference actionEventHandler;
    private final ThreadLocal<InternalEventPipeline> dispatchingPipeline
        = new ThreadLocal<>();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ThreadLocal<InternalEventPipeline> currentPipeline
        = new ThreadLocal<>();

    /** The Constant DUMMY_HANDLER. */
    public static final ComponentVertex DUMMY_HANDLER = new DummyComponent();

    static {
        ErrorPrinter errorPrinter = new ErrorPrinter();
        try {
            fallbackErrorHandler = new HandlerReference(errorPrinter,
                ErrorPrinter.class.getMethod("printError", Error.class),
                0, new HandlerScope() {
                    @Override
                    public boolean includes(
                            Eligible event, Eligible[] channels) {
                        return true;
                    }
                });
        } catch (NoSuchMethodException | SecurityException e) {
            // Doesn't happen, but just in case
            e.printStackTrace(); // NOPMD, won't define a logger for that.
        }

        ActionExecutor actionExecutor = new ActionExecutor();
        try {
            actionEventHandler = HandlerReference.newRef(actionExecutor,
                ActionExecutor.class.getMethod("execute", ActionEvent.class),
                0, new HandlerScope() {
                    @Override
                    public boolean includes(
                            Eligible event, Eligible[] channels) {
                        return true;
                    }

                    @Override
                    public String toString() {
                        return "wildcard";
                    }
                });
        } catch (NoSuchMethodException | SecurityException e) {
            // Doesn't happen, but just in case
            e.printStackTrace(); // NOPMD, won't define a logger for that.
        }
    }

    /*
     * This could simply be declared as an anonymous class. But then
     * "Find bugs" complains about the noop() not being callable, because it
     * doesn't consider that it is called by reflection.
     */
    /**
     * A dummy component.
     */
    private static class DummyComponent extends Component {

        /**
         * Instantiates a new dummy component.
         */
        public DummyComponent() {
            super(Channel.SELF);
        }

        /**
         * Dummy handler does nothing.
         * 
         * @param event the event
         */
        @Handler(channels = { Channel.class })
        public void noop(Event<?> event) {
            // ... really nothing.
        }
    }

    /**
     * Creates a new tree for the given node or sub tree.
     * 
     * @param root the root node of the new tree
     */
    /* default */ ComponentTree(ComponentVertex root) {
        super();
        this.root = root;
    }

    /* default */ ComponentVertex root() {
        return root;
    }

    /* default */ boolean isStarted() {
        return !(eventPipeline instanceof BufferingEventPipeline);
    }

    /* default */ void setEventPipeline(InternalEventPipeline pipeline) {
        eventPipeline = pipeline;
    }

    /* default */ InternalEventPipeline eventPipeline() {
        return eventPipeline;
    }

    /**
     * Sets the the currently dispatching pipeline. Must be called by
     * an event processors before invoking 
     * {@link #dispatch(EventPipeline, EventBase, Channel[])}.
     *
     * @param pipeline the new dispatching pipeline
     */
    public void setDispatchingPipeline(InternalEventPipeline pipeline) {
        dispatchingPipeline.set(pipeline);
        currentPipeline.set(pipeline);
    }

    /**
     * Gets the the event pipeline that currently dispatches events to
     * this tree.
     *
     * @return the dispatching pipeline
     */
    public InternalEventPipeline dispatchingPipeline() {
        return dispatchingPipeline.get();
    }

    /**
     * Gets the the pipeline associated with the currently executing
     * thread.
     *
     * @return the dispatching pipeline
     */
    public static InternalEventPipeline currentPipeline() {
        return currentPipeline.get();
    }

    /**
     * Adds the event to the tree's event pipeline.
     *
     * @param event the event
     * @param channels the channels
     */
    @SuppressWarnings("PMD.UseVarargs")
    public void fire(Event<?> event, Channel[] channels) {
        eventPipeline.add(event, channels);
    }

    /**
     * Merge all events stored with <code>source</code> with our own.
     * This is invoked if a component (or component tree) is attached 
     * to another tree (that uses this component common).
     * 
     * @param source
     */
    /* default */ void mergeEvents(ComponentTree source) {
        eventPipeline.merge(source.eventPipeline);
    }

    /**
     * Execute all matching handlers.
     * 
     * @param event the event
     * @param channels the channels the event is sent to
     */
    @SuppressWarnings("PMD.UseVarargs")
    public void dispatch(EventPipeline pipeline,
            EventBase<?> event, Channel[] channels) {
        HandlerList handlers = getEventHandlers(event, channels);
        handlers.process(pipeline, event);
    }

    @SuppressWarnings("PMD.UseVarargs")
    private HandlerList getEventHandlers(
            EventBase<?> event, Channel[] channels) {
        CacheKey key = new CacheKey(event, channels);
        // Several event processors may call dispatch and update the cache
        // concurrently, and the cache may be cleared by a concurrent call
        // to detach.
        HandlerList hdlrs = handlerCache.get(key);
        if (hdlrs != null) {
            return hdlrs;
        }
        // Don't allow tree modifications while collecting
        synchronized (this) {
            // Optimization for highly concurrent first-time access
            // with the same key: another thread may have created the
            // handlers while this one was waiting for the lock.
            hdlrs = handlerCache.get(key);
            if (hdlrs != null) {
                return hdlrs;
            }
            hdlrs = new HandlerList();
            root.collectHandlers(hdlrs, event, channels);
            if (hdlrs.isEmpty()) {
                // Make sure that errors are reported.
                if (event instanceof Error) {
                    hdlrs.add(fallbackErrorHandler);
                    // Handle (internal) action events
                } else if (event instanceof ActionEvent) {
                    hdlrs.add(actionEventHandler);
                } else {
                    if (handlerTracking.isLoggable(Level.FINER)) {
                        DUMMY_HANDLER.collectHandlers(hdlrs, event,
                            channels);
                    }
                }
            }
            Collections.sort(hdlrs);
            handlerCache.put(key, hdlrs);
        }
        return hdlrs;
    }

    /* default */ void clearHandlerCache() {
        synchronized (this) {
            handlerCache.clear();
        }
    }

    /**
     * An artificial key for handler caching.
     */
    private static class CacheKey {
        private final Object eventMatchValue;
        private final Object[] channelMatchValues;

        /**
         * Instantiates a new cache key.
         *
         * @param event the event
         * @param channels the channels
         */
        public CacheKey(EventBase<?> event, Channel... channels) {
            eventMatchValue = event.defaultCriterion();
            channelMatchValues = new Object[channels.length];
            for (int i = 0; i < channels.length; i++) {
                channelMatchValues[i] = channels[i].defaultCriterion();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
        public int hashCode() {
            @SuppressWarnings("PMD.AvoidFinalLocalVariable")
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(channelMatchValues);
            result = prime * result + ((eventMatchValue == null) ? 0
                : eventMatchValue.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        @SuppressWarnings("PMD.SimplifyBooleanReturns")
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
            CacheKey other = (CacheKey) obj;
            if (eventMatchValue == null) {
                if (other.eventMatchValue != null) {
                    return false;
                }
            } else if (!eventMatchValue.equals(other.eventMatchValue)) {
                return false;
            }
            if (!Arrays.equals(channelMatchValues, other.channelMatchValues)) {
                return false;
            }
            return true;
        }
    }

}
