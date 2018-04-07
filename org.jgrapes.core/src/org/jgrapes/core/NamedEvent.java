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

package org.jgrapes.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for events using a simple name as the event's kind.
 */
public final class NamedEvent<T> extends Event<T> {

    private final String kind;
    private Map<Object, Object> data;

    /**
     * Creates a new named event with the given name.
     * 
     * @param kind the event's kind
     */
    public NamedEvent(String kind) {
        super();
        this.kind = kind;
    }

    /**
     * Returns the kind of the event as the String passed to the
     * constructor.
     * 
     * @return the kind
     * 
     * @see org.jgrapes.core.Channel#defaultCriterion()
     */
    @Override
    public Object defaultCriterion() {
        return kind;
    }

    /**
     * Returns `true` if the criterion is `Event.class` (representing 
     * "any event") or if the criterion is a String equal to this 
     * event's kind (the String passed to the constructor).
     * 
     * @see org.jgrapes.core.Eligible#isEligibleFor(java.lang.Object)
     */
    @Override
    public boolean isEligibleFor(Object criterion) {
        return criterion.equals(Event.class) || criterion.equals(kind);
    }

    /**
     * Returns a map with data that belongs to the event. The map
     * is only created if requested. If a component uses
     * {@link NamedEvent}s and data that consists of JDK types only,
     * it is completely loosely coupled.
     * 
     * @return the map
     */
    public Map<Object, Object> data() {
        if (data == null) {
            data = new HashMap<>();
        }
        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(50);
        result.append("NamedEvent [name=")
            .append(kind);
        if (channels().length > 0) {
            result.append(", channels=")
                .append(Arrays.toString(channels()));
        }
        result.append(']');
        return result.toString();
    }
}
