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
import org.jgrapes.io.FileStorage;

/**
 * Causes the {@link FileStorage} component to write the data from all
 * {@link Output} events on the channel that this event is fired on to a file
 * until an event with the end of record flag set is sent on the channel.
 */
public class SaveOutput extends OpenFile {

    /**
     * Creates a new instance.
     * 
     * @param path the file's path
     * @param options open options
     */
    public SaveOutput(Path path, OpenOption... options) {
        super(path, options);
    }
}
