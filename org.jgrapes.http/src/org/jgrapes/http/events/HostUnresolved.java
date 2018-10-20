/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.http.events;

import org.jgrapes.core.Event;
import org.jgrapes.core.events.Error;

/**
 * Indicates that a host name could not be resolved.
 */
@SuppressWarnings("PMD.DoNotExtendJavaLangError")
public class HostUnresolved extends Error {

    /**
     * Creates a new event as a copy of an existing event. Useful
     * for forwarding an event.
     *  
     * @param event
     */
    public HostUnresolved(HostUnresolved event) {
        super(event);
    }

    /**
     * @param event
     * @param message
     */
    public HostUnresolved(Event<?> event, String message) {
        super(event, message);
    }
}
