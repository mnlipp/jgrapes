/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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

import java.nio.Buffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

/**
 * @author Michael N. Lipp
 *
 */
public class FileOpened<T extends Buffer> extends Opened {

	private Path path;
	private OpenOption[] options;
	private BlockingQueue<T> buffers;
	
	public FileOpened(Path path, OpenOption[] options, 
			BlockingQueue<T> buffers) {
		super();
		this.path = path;
		this.options = options;
		this.buffers = buffers;
	}

	public FileOpened(Path path, OpenOption[] options) {
		this(path, options, null);
	}

	/**
	 * @return the path
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @return the options
	 */
	public OpenOption[] getOptions() {
		return options;
	}

	/**
	 * @return the buffers
	 */
	public BlockingQueue<T> getBuffers() {
		return buffers;
	}
	
}
