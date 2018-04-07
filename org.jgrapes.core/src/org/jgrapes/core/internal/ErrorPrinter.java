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

package org.jgrapes.core.internal;

import org.jgrapes.core.ComponentType;
import org.jgrapes.core.events.Error;

/**
 * A fallback error printer.
 */
public class ErrorPrinter implements ComponentType {

    /**
     * Prints the error.
     *
     * @param event the event
     */
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
        "PMD.AvoidCatchingGenericException",
        "PMD.UseStringBufferForStringAppends", "PMD.SystemPrintln" })
    public void printError(Error event) {
        String msg = "(No event)";
        if (event.event() != null) {
            try {
                msg = event.event().toString();
            } catch (Exception t) {
                msg = "(Cannot convert event to string: " + t.getMessage()
                    + ")";
            }
        }
        msg += ": " + (event.message() == null ? "(No message)"
            : event.message());
        System.err.println(msg);
        if (event.throwable() == null) {
            System.err.println("No stack trace available.");
        } else {
            event.throwable().printStackTrace();
        }
    }

}
