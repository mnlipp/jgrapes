/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
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

package org.jgrapes.io.util.events;

import org.jgrapes.core.events.Error;

/**
 * Signals an error that occurred while parsing JSON.
 */
public class JsonParsingError extends Error {

    /**
     * Instantiates a new JSON parsing error.
     *
     * @param throwable the throwable
     */
    public JsonParsingError(Throwable throwable) {
        super(null, throwable);
    }

}
