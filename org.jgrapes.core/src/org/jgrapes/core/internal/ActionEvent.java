/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
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

import java.util.concurrent.Callable;

import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;

/**
 * A base class for events that perform an action instead of being
 * handled. Use to synchronize some action with th eother events on
 * an {@link EventPipeline}.
 */
public abstract class ActionEvent<T> extends Event<T> {

    private static Object defaultCriterion = new Object();

    /* default */ static <V> ActionEvent<V> create(Callable<V> action) {
        return new CallableActionEvent<>(action);
    }

    /* default */ static ActionEvent<Void> create(Runnable action) {
        return new RunnableActionEvent(action);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    /* default */ abstract void execute() throws Exception;

    @Override
    public boolean isEligibleFor(Object criterion) {
        return criterion == defaultCriterion; // NOPMD, comparing references
    }

    @Override
    public Object defaultCriterion() {
        return defaultCriterion;
    }

    /**
     * An {@link ActionEvent} that executes a {@link Callable}.
     *
     * @param <V> the value type
     */
    private static class CallableActionEvent<V> extends ActionEvent<V> {
        private final Callable<V> action;

        /**
         * Instantiates a new callable action event.
         *
         * @param callable the callable
         */
        public CallableActionEvent(Callable<V> callable) {
            this.action = callable;
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        /* default */ void execute() throws Exception {
            setResult(action.call());
        }
    }

    /**
     * An {@link ActionEvent} that executes a {@link Runnable}.
     */
    private static class RunnableActionEvent extends ActionEvent<Void> {
        private final Runnable action;

        /**
         * Instantiates a new runnable action event.
         *
         * @param action the action
         */
        public RunnableActionEvent(Runnable action) {
            this.action = action;
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        /* default */ void execute() throws Exception {
            action.run();
        }

    }
}
