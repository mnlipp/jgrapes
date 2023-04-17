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

package org.jgrapes.io.events;

import org.jgrapes.core.Event;

/**
 * Indicates a problem while starting a process.
 */
public class StartProcessError extends IOError {

    /**
     * Creates a new event as a copy of an existing event. Useful
     * for forwarding an event.
     *  
     * @param event
     */
    public StartProcessError(StartProcessError event) {
        super(event);
    }

    /**
     * @param event
     * @param message
     */
    public StartProcessError(Event<?> event, String message) {
        super(event, message);
    }

    /**
     * @param event
     * @param message
     * @param throwable
     */
    public StartProcessError(Event<?> event, String message,
            Throwable throwable) {
        super(event, message, throwable);
    }

    /**
     * @param event
     * @param throwable
     */
    public StartProcessError(Event<?> event, Throwable throwable) {
        super(event, throwable);
    }
}
