/*
 * JGrapes Event driven Framework
 * Copyright (C) 2023 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jgrapes.io.events;

/**
 * Fired when a process has been started.
 */
public class ProcessStarted extends Opened<StartProcess> {

    /**
     * Instantiates a new event.
     *
     * @param startEvent the start event
     */
    public ProcessStarted(StartProcess startEvent) {
        setResult(startEvent);
    }

    /**
     * Returns the event that started the process.
     * 
     * @return the event
     */
    public StartProcess startEvent() {
        return currentResults().get(0);
    }
}
