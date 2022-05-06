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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jgrapes.core.Associator;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionEvent;
import org.jgrapes.core.CompletionLock;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * Provides the implementations of methods to class {@link Event} that
 * need access to classes or methods that are visible in the implementation
 * package only. The class is not intended to be used as base class
 * for any other class.
 * 
 * @param <T> the result type of the event. Use {@link Void} if handling
 * the event does not produce a result
 */
public abstract class EventBase<T>
        implements Eligible, Future<T>, Associator {

    /** The event that caused this event. */
    private EventBase<?> generatedBy;
    /** Number of events that have to be processed until completion.
     * This is one for the event itself and one more for each event
     * that has this event as its cause. */
    private final AtomicInteger openCount = new AtomicInteger(1);
    /** Completion locks. */
    private Set<CompletionLockBase> completionLocks;
    /** Set when the event is enqueued, reset when it has been completed. */
    private EventProcessor processedBy;
    /** The events to be fired upon completion. Using this attribute
     * provides a slightly faster access than invoking
     * {@link Event#completionEvents()}, which wraps the result in
     * an unmodifiable set. */
    protected Set<Event<?>> completionEvents;
    /** Set when the event has been completed. */
    protected boolean completed;
    private boolean requiresResult;
    /** Event is tracked by {@link VerboseHandlerReference}. */
    private boolean tracked = true;

    /**
     * See {@link Event#channels()}.
     *
     * @return the channel[]
     */
    public abstract Channel[] channels();

    /**
     * See {@link Event#handled()}.
     */
    protected abstract void handled();

    /**
     * See {@link Event#isStopped()}.
     */
    public abstract boolean isStopped();

    /**
     * See {@link Event#currentResults()}.
     */
    protected abstract List<T> currentResults();

    /**
     * See {@link Event#setRequiresResult(boolean)}.
     */
    protected EventBase<T> setRequiresResult(boolean value) {
        if (requiresResult == value) {
            return this;
        }
        if (value) {
            openCount.incrementAndGet();
            requiresResult = true;
        } else {
            requiresResult = false;
            decrementOpen();
        }
        return this;
    }

    /**
     * See {@link Event#firstResultAssigned()}.
     */
    protected void firstResultAssigned() {
        if (requiresResult) {
            decrementOpen();
        }
    }

    /**
     * Returns <code>true</code> if the event has been enqueued in a pipeline.
     * 
     * @return the result
     */
    protected boolean enqueued() {
        return processedBy != null || completed || isCancelled();
    }

    /**
     * Invoked when an exception occurs while invoking a handler for an event.
     * 
     * @param eventProcessor the manager that has invoked the handler
     * @param throwable the exception that has been thrown by the handler
     */
    protected abstract void handlingError(
            EventPipeline eventProcessor, Throwable throwable);

    /**
     * If an event is fired while processing another event, note
     * the event being processed. This allows us to track the cause
     * of events to the "initial" (externally) generated event that
     * triggered everything.
     * 
     * @param causingEvent the causing event to set
     */
    /* default */ void generatedBy(EventBase<?> causingEvent) {
        generatedBy = causingEvent;
        if (causingEvent != null) {
            causingEvent.openCount.incrementAndGet();
        }
    }

    /**
     * Set the processor that will (eventually) process the event. 
     * 
     * @param processor the processor
     */
    /* default */ void processedBy(EventProcessor processor) {
        this.processedBy = processor;
    }

    public Optional<EventPipeline> processedBy() {
        return Optional.ofNullable(processedBy).map(
            procBy -> procBy.asEventPipeline());
    }

    /**
     * @param pipeline
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    /* default */ void decrementOpen() {
        if (openCount.decrementAndGet() == 0 && !completed) {
            synchronized (this) {
                completed = true;
                notifyAll();
            }
            if (completionEvents != null && !isCancelled()) {
                processedBy.updateNewEventsParent(generatedBy);
                for (Event<?> e : completionEvents) {
                    Channel[] completeChannels = e.channels();
                    if (completeChannels.length == 0) {
                        // Note that channels() cannot be empty, as it is set
                        // when firing the event and an event is never fired
                        // on no channels.
                        completeChannels = channels();
                        e.setChannels(completeChannels);
                    }
                    processedBy.add(e, completeChannels);
                }
            }
            if (generatedBy != null) {
                generatedBy.decrementOpen();
            }
            processedBy = null; // No longer needed
        }
    }

    /**
     * Adds the given completion lock. 
     * 
     * @param lock the lock
     * @see CompletionLock
     */
    /* default */ Event<T> addCompletionLock(CompletionLockBase lock) {
        synchronized (this) {
            if (completionLocks == null) {
                completionLocks = Collections.synchronizedSet(new HashSet<>());
            }
        }
        if (completionLocks.add(lock)) {
            openCount.incrementAndGet();
            lock.startTimer();
        }
        return (Event<T>) this;
    }

    /**
     * Removes the given completion lock. 
     * 
     * @param lock the lock
     * @see CompletionLock
     */
    /* default */ void removeCompletionLock(CompletionLockBase lock) {
        if (completionLocks == null) {
            return;
        }
        if (completionLocks.remove(lock)) {
            decrementOpen();
        }
        lock.cancelTimer();
    }

    /**
     * Disables tracking for this event and all events generated
     * when handling it.
     */
    public Event<T> disableTracking() {
        tracked = false;
        return (Event<T>) this;
    }

    /**
     * Whether the event (and all events generated when handling it)
     * is tracked.
     * 
     * @return `true` if event is tracked
     */
    public boolean isTracked() {
        return tracked;
    }

    @SuppressWarnings("PMD.UselessParentheses")
    /* default */ boolean isTrackable() {
        return generatedBy == null ? tracked
            : (tracked && generatedBy.isTrackable());
    }

    /**
     * Adds the given event to the events to be thrown when this event 
     * has completed (see {@link #isDone()}). Such an event is called 
     * a "completion event".
     * 
     * Completion events are considered to be caused by the event that 
     * caused the completed event. If an event *e1* caused an event
     * *e2* which has a completion event *e2c*, *e1* is only put in 
     * state completed when *e2c* has been handled.
     * 
     * Completion events are handled by the same {@link EventProcessor}
     * as the event that has been completed.
     * 
     * @param completionEvent the completion event to add
     * @return the object for easy chaining
     * @see #onCompletion(Event, Consumer)
     */
    public abstract Event<T> addCompletionEvent(Event<?> completionEvent);

    /**
     * Invokes the consumer when the event is completed. This is
     * a shortcut for registering a {@link CompletionEvent} and
     * providing a handler for the completion event that invokes 
     * the consumer.
     * 
     * The static form is required because otherwise the compiler cannot
     * infer the type of the consumer's argument.
     *
     * @param <T> the result type of the event
     * @param <E> the type of the event
     * @param event the event
     * @param consumer the consumer
     * @return the event
     */
    public static <T, E extends Event<T>> E onCompletion(E event,
            Consumer<E> consumer) {
        event.addCompletionEvent(new ActionEvent<Event<T>>(
            event.getClass().getSimpleName() + "CompletionAction") {
            @Override
            public void execute() throws Exception {
                consumer.accept(event);
            }
        });
        return event;
    }
}
