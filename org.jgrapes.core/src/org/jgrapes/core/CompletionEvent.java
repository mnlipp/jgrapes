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

import org.jgrapes.core.internal.EventBase;

/**
 * A base class for events that signal the completion of some other
 * (monitored) event and provide this other event as their result. 
 * Events of this type are automatically fired when the framework
 * detects that the monitored event has completed.  
 * 
 * Use {@link #event()} to conveniently access the monitored event 
 * while handling the completion event. 
 * 
 * @see EventBase#onCompletion(Event, java.util.function.Consumer)
 * @see EventBase#addCompletionEvent(Event)
 */
public abstract class CompletionEvent<T extends Event<?>>
        extends Event<T> {

    /**
     * Instantiates a new completion event.
     *
     * @param monitoredEvent the monitored event
     * @param channels the channels
     */
    public CompletionEvent(T monitoredEvent, Channel... channels) {
        super(channels);
        setResult(monitoredEvent);
        monitoredEvent.addCompletionEvent(this);
    }

    /**
     * Return the completed event. This is simply a shortcut 
     * for ``currentResults().get(0)``.
     * 
     * @return the completed event
     */
    public T event() {
        return currentResults().get(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Components.className(getClass()))
            .append('(')
            .append(Components.objectName(currentResults().get(0)))
            .append(") [");
        if (channels().length > 0) {
            builder.append("channels=");
            builder.append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }
}
