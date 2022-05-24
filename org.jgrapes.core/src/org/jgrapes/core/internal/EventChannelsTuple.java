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
import java.util.Queue;
import org.jgrapes.core.Channel;

/**
 * This class provides a container for an event and an arbitrary 
 * number of channels. Instances represent a particular event being fired
 * on several channels. They are used e.g. to queue the information
 * about an event being fired on some channels.
 */
public class EventChannelsTuple {
    public EventBase<?> event;
    public Channel[] channels;

    /**
     * Create a new instance.
     * 
     * @param event the event
     * @param channels the channels
     */
    @SuppressWarnings("PMD.UseVarargs")
    public EventChannelsTuple(EventBase<?> event, Channel[] channels) {
        super();
        this.event = event;
        this.channels = Arrays.copyOf(channels, channels.length);
    }

    /**
     * Adds a newly created {@link EventChannelsTuple} to the given queue.
     *
     * @param queue the queue
     * @param event the event
     * @param channels the channels
     */
    public static void addTo(Queue<EventChannelsTuple> queue,
            EventBase<?> event, Channel... channels) {
        queue.add(new EventChannelsTuple(event, channels));
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
        result = prime * result + Arrays.hashCode(channels);
        result = prime * result + ((event == null) ? 0 : event.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
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
        EventChannelsTuple other = (EventChannelsTuple) obj;
        if (!Arrays.equals(channels, other.channels)) {
            return false;
        }
        if (event == null) {
            if (other.event != null) {
                return false;
            }
        } else if (!event.equals(other.event)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(50);
        builder.append("EventChannelsTuple [");
        if (event != null) {
            builder.append("event=")
                .append(event)
                .append(", ");
        }
        if (channels != null) {
            builder.append("channels=")
                .append(Arrays.toString(channels));
        }
        builder.append(']');
        return builder.toString();
    }

}