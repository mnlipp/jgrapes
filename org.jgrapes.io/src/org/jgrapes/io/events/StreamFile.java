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

package org.jgrapes.io.events;

import java.nio.file.OpenOption;
import java.nio.file.Path;

import org.jgrapes.core.Event;

/**
 * Causes the content of a file to be streamed as a sequence of {@link Output}
 * events (terminated by an event with the end of record flag set) on the 
 * channel that this event is fired on.
 */
public class StreamFile extends Event<Void> {

	private Path path;
	private OpenOption[] options;

	/**
	 * Creates a new instance.
	 * 
	 * @param path the file's path
	 * @param options open options
	 */
	public StreamFile(Path path, OpenOption... options) {
		this.path = path;
		this.options = options;
	}

	/**
	 * Returns the event's path.
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
		return options;
	}

}
