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
import org.jgrapes.core.CompletionEvent;

/**
 * The event that signals the completion of the {@link Start} event.
 */
public class Started extends CompletionEvent<Start> {

    /**
     * Instantiates a new {@link Started} event.
     *
     * @param monitoredEvent the monitored event
     * @param channels the channels
     */
    public Started(Start monitoredEvent, Channel... channels) {
        super(monitoredEvent, channels);
    }

}
