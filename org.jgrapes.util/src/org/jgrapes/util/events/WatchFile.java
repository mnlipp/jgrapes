/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2023 Michael N. Lipp
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

package org.jgrapes.util.events;

import java.nio.file.Path;
import org.jgrapes.core.Event;

/**
 * Registers a path to be watched with a {@link FileSystemWatcher}.
 */
public class WatchFile extends Event<Void> {

    private final Path path;

    /**
     * Creates a new instance.
     *
     * @param path the file's path
     * @param kinds the watch kinds
     */
    public WatchFile(Path path) {
        this.path = path;
    }

    /**
     * Return's the event's path. 
     * 
     * @return the path
     */
    public Path path() {
        return path;
    }
}
