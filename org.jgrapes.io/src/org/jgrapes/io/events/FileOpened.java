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

package org.jgrapes.io.events;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

/**
 *
 */
public class FileOpened extends Opened<OpenFile> {

    private final Path path;
    private final OpenOption[] options;

    /**
     * Instantiates a new event, using the values for path and
     * options from the opening event. 
     *
     * @param event the event that caused opening the file
     */
    public FileOpened(OpenFile event) {
        setResult(event);
        this.path = event.path();
        this.options = event.options();
    }

    /**
     * Instantiates a new event, overriding the values using the
     * given values for path and options.
     *
     * @param event the event that caused opening the file
     * @param path the path
     * @param options the options
     */
    public FileOpened(OpenFile event, Path path, OpenOption... options) {
        setResult(event);
        this.path = path;
        this.options = Arrays.copyOf(options, options.length);
    }

    /**
     * Returns the event that caused the file to be opened.
     * 
     * @return the event
     */
    public OpenFile openEvent() {
        return currentResults().get(0);
    }

    /**
     * @return the path
     */
    public Path path() {
        return path;
    }

    /**
     * @return the options
     */
    public OpenOption[] options() {
        return Arrays.copyOf(options, options.length);
    }

}
