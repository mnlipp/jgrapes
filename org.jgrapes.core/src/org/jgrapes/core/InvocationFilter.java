/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2019 Michael N. Lipp
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
 * Some {@link HandlerScope} implementations use only a subset of the
 * properties of the events passed to 
 * {@link HandlerScope#includes(Eligible, Eligible[])} for 
 * evaluating the result. This is done in order to avoid an excessive 
 * number of entries in the table that maps events (and channels) to
 * handlers.    
 * 
 * When dispatching an event, the superfluous values from the mapping table
 * must be filtered. {@link HandlerScope} implementations that do not
 * take all properties of an event into account in their implementation
 * of {@link HandlerScope#includes(Eligible, Eligible[])} must therefore
 * also implement this interface.
 */
public interface InvocationFilter {

    /**
     * Matches the given event against the criteria
     * for events, taking properties into account that were left out
     * by {@link HandlerScope#includes(Eligible, Eligible[])}.
     * 
     * Note that this method is called only with events that have passed 
     * the invocation of {@link HandlerScope#includes(Eligible, Eligible[])}.
     * It is therefore not required to re-test conditions already
     * checked by {@link HandlerScope#includes(Eligible, Eligible[])}.
     *
     * @param event the event
     * @return true, if successful
     */
    boolean includes(EventBase<?> event);
}
