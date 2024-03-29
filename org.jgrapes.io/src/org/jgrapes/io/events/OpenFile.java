/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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
import org.jgrapes.core.Event;

/**
 * A base class for events that cause a file to be opened.
 */
public class OpenFile extends Event<Void> {

    private final Path path;
    private final OpenOption[] options;

    /**
     * Creates a new instance.
     * 
     * @param path the file's path
     * @param options open options
     */
    public OpenFile(Path path, OpenOption... options) {
        this.path = path;
        this.options = Arrays.copyOf(options, options.length);
    }

    /**
     * Return's the event's path. 
     * 
     * @return the path
     */
    public Path path() {
        return path;
    }

    /**
     * Returns the event's options.
     * 
     * @return the options
     */
    public OpenOption[] options() {
        return Arrays.copyOf(options, options.length);
    }
}
