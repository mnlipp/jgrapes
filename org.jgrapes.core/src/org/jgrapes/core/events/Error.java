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

package org.jgrapes.core.events;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * This event signals that an error occurred while processing an event.
 */
public class Error extends Event<Void> {

    private final Event<?> event;
    private final String message;
    private Throwable throwable;

    /**
     * Creates a new event as a copy of an existing event. Useful
     * for forwarding an event.
     *
     * @param event the event to copy
     */
    public Error(Error event) {
        this.event = event.event;
        this.message = event.message;
        this.throwable = event.throwable;
    }

    /**
     * Creates a new event.
     * 
     * @param event the event being processed when the problem occurred
     * @param message the message
     */
    public Error(Event<?> event, String message) {
        this.event = event;
        this.message = message;
    }

    /**
     * Creates a new event caused by the given throwable.
     * 
     * @param event the event being processed when the problem occurred
     * @param message the message
     * @param throwable the throwable
     */
    public Error(Event<?> event, String message, Throwable throwable) {
        this.event = event;
        this.message = message;
        this.throwable = throwable;
    }

    /**
     * Creates a new event caused by the given throwable. The message
     * is initialized from the throwable.
     * 
     * @param event the event being processed when the problem occurred
     * @param throwable the throwable
     */
    public Error(Event<?> event, Throwable throwable) {
        this.event = event;
        this.message = throwable.getMessage() == null
            ? throwable.getClass().getName()
            : throwable.getMessage();
        this.throwable = throwable;
    }

    /**
     * Returns the event that was handled when the problem occurred.
     * 
     * @return the event
     */
    public Event<?> event() {
        return event;
    }

    /**
     * Returns the message passed to the constructor.
     * 
     * @return the message
     */
    public String message() {
        return message;
    }

    /**
     * Returns the throwable that caused the problem.
     * 
     * @return the throwable or {@code null} if the problem wasn't caused
     * by a throwable.
     */
    public Throwable throwable() {
        return throwable;
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
        if (channels().length > 0) {
            builder.append("channels=");
            builder.append(Channel.toString(channels()));
        }
        if (message != null) {
            builder.append(", message=\"");
            builder.append(message);
            builder.append('"');
        }
        builder.append(']');
        return builder.toString();
    }
}
