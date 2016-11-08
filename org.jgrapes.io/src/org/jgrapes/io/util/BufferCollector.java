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
package org.jgrapes.io.util;

/**
 * Defines the method of a buffer collector.
 * 
 * @author Michael N. Lipp
 */
public interface BufferCollector {

	/**
	 * A predefined buffer collector that does nothing when the managed buffer
	 * is no longer used. Using this collector with a managed buffer
	 * effectively make it unmanaged buffer.
	 */
	BufferCollector NOOP_COLLECTOR 
		= new BufferCollector() {
			@Override
			public void recollect(ManagedBuffer<?> buffer) {
			}
		};

	/**
	 * Recollect the buffer. Invoked after all locks to a managed buffer
	 * have been released. Usually, the implementation of a buffer collector
	 * returns the buffer into some kind of pool when this method is invoked.
	 * 
	 * @param buffer the buffer
	 */
	void recollect (ManagedBuffer<?> buffer);
	
}
