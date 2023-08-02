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
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;

/**
 * Informs about changes of a watched file.
 */
public class FileChanged extends Event<Void> {

    /**
     * The kind of change detected.
     */
    public enum Kind {
        CREATED, MODIFIED, DELETED
    }

    private final Path path;
    private final Kind change;

    /**
     * Instantiates a new event.
     *
     * @param path the watched path
     * @param change the change
     */
    public FileChanged(Path path, Kind change) {
        this.path = path;
        this.change = change;
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
     * Returns the change.
     * 
     * @return the change
     */
    public Kind change() {
        return change;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Components.objectName(this))
            .append(" [").append(change).append(": ").append(path);
        if (channels() != null) {
            builder.append(", channels=");
            builder.append(Channel.toString(channels()));
        }
        builder.append(']');
        return builder.toString();
    }
}
