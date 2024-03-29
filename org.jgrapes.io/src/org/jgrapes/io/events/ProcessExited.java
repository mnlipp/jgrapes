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
 * Fired when a process has exited.
 */
public class ProcessExited extends Closed<Integer> {

    private final StartProcess startedBy;

    /**
     * Instantiates a new event.
     *
     * @param startedBy the event that started the process
     * @param exitValue the exit value
     */
    public ProcessExited(StartProcess startedBy, int exitValue) {
        setResult(exitValue);
        this.startedBy = startedBy;
    }

    /**
     * Returns the event that started the terminated process.
     * 
     * @return the startedBy
     */
    public StartProcess startedBy() {
        return startedBy;
    }

    /**
     * Convenience method to access the exit value without calling
     * {@link #get()} (which may block). 
     *
     * @return the exit value of the process
     */
    public int exitValue() {
        return currentResults().get(0);
    }

}
