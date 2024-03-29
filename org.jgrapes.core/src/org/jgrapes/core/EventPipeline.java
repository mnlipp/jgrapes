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

package org.jgrapes.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jgrapes.core.Components.IdInfoProvider;

/**
 * An interface that describes a queue of events that are sent to the components
 * of the associated tree. Any events fired by the components while handling
 * an event from the pipeline are added at the end of the pipeline.
 * 
 * An event pipeline is run by a single thread from an executor service. 
 * Adding several events to the same pipeline therefore ensures that they 
 * are executed in sequence.
 */
public interface EventPipeline extends IdInfoProvider {

    /**
     * Add an event to be sent to components listening for such events on
     * the given channels to the end of the queue. If no channels are
     * specified as parameters, the event is fired on the event's 
     * channel (see {@link Event#channels()}). If the event doesn't
     * specify channels either, the channel depends on how the
     * event pipeline was obtained. Event pipelines obtained from a
     * component's manager use the component's channel as fall back.
     * 
     * @param <T> the event's type
     * @param event the event to process
     * @param channels the channels that the event was fired on
     * @return the event (for easy chaining)
     */
    <T extends Event<?>> T fire(T event, Channel... channels);

    /**
     * Allow only the given source pipeline to fire events on this
     * pipeline.
     * 
     * This feature can be used to ensure the proper usage of event
     * pipelines. Assume that a handler invoked from event pipeline
     * *S* produces several related output events and fires them
     * using another event pipeline *O*. In this case, it is important
     * that the proper sequence is not disturbed. By restricting
     * the source pipeline of *O* to *S*, it becomes impossible
     * for other threads than the one that runs the event pipeline
     * *S* to fire events that are to be processed by *O*.
     * 
     * Events from other threads than the one that runs *O*
     * are ignored and an error is logged with the core package
     * logger with ".fireRestriction" appended.
     *
     * @param sourcePipeline the source pipeline or `null` to
     * revoke the restriction
     * @return the event pipeline
     */
    EventPipeline restrictEventSource(EventPipeline sourcePipeline);

    /**
     * Overrides any restriction set by 
     * {@link #restrictEventSource(EventPipeline)} for the next 
     * {@link #fire(Event, Channel...)} invocation from the calling thread.
     * 
     * The typical use case is a protocol handling component
     * (see the JGrapes I/O package) . After a connection and an 
     * associated IOSubchannel have been established, the response 
     * pipeline is usually only used by the downstream component,
     * which may restrict the source for events on that pipeline.
     * 
     * Sometimes, however, the protocol handling component must insert
     * events with out-of-band (control) information in the stream
     * of events that form the regular I/O data. This method allows
     * the protocol component to do this, even if a restriction
     * applies to the response pipeline. 
     *
     * @return the event pipeline
     */
    EventPipeline overrideRestriction();

    /**
     * Adds an action to be executed to the event pipeline. 
     * Execution of the action is synchronized with the events
     * on this pipeline. It will be executed after any events
     * already fired and before any event fired subsequently.
     * 
     * This is a short-cut for firing a special kind of event 
     * and defining a handler for only this kind of event.
     * Submitting a callable instead of firing an event and defining
     * a handler should only be done when no use case is imaginable
     * in which such an event could be intercepted by other 
     * components or could trigger some action.
     *
     * @param <V> the value type
     * @param name the name of the action; used in the event debug log 
     * @param action the action to execute
     * @return the future
     */
    <V> Future<V> submit(String name, Callable<V> action);

    /**
     * Like {@link #submit(String, Callable)} but without specifying a
     * name for the event debug log.
     * 
     * @param action the action to execute
     */
    default <V> Future<V> submit(Callable<V> action) {
        return submit(null, action);
    }

    /**
     * Adds an action to be executed to the event pipeline. 
     * Execution of the action is synchronized with the events
     * on this pipeline. It will be executed after any events
     * already fired and before any event fired subsequently.
     * 
     * This is a short-cut for firing a special kind of event 
     * and defining a handler for only this kind of event.
     * Submitting a runnable instead of firing an event and defining
     * a handler should only be done when no use case is imaginable
     * in which such an event could be intercepted by other 
     * components or could trigger some action.
     *
     * @param name the name of the action; used in the event debug log 
     * @param action the action to execute
     */
    void submit(String name, Runnable action);

    /**
     * Like {@link #submit(String, Callable)} but without specifiying
     * a name for the event debug log.
     * 
     * @param action the action to execute
     */
    default void submit(Runnable action) {
        submit(null, action);
    }

    /**
     * All pipelines use the same id scope to make them uniquely identifiable
     * by their number.
     */
    @Override
    default Class<?> idScope() {
        return EventPipeline.class;
    }

    /**
     * Returns the executor service used by this event pipeline to
     * obtain its thread. If a component needs to create a thread for its
     * own use, it should obtain it from the component's event pipeline's
     * executor service in order to conform with standard resource
     * management.
     * 
     * @return the executor service
     */
    ExecutorService executorService();

    /**
     * Waits until this pipeline is empty. 
     *
     * @throws InterruptedException
     */
    void awaitExhaustion() throws InterruptedException;
}
