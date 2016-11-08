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

import org.jgrapes.core.Event;
import org.jgrapes.core.events.Error;

/**
 * A special kind of {@link Error} that signals problems during IO.
 * 
 * @author Michael N. Lipp
 */
public class IOError extends Error {

	/**
	 * Creates a new instance.
	 * 
	 * @param event the event that was being handled when the problem occurred
	 * @param message the message
	 */
	public IOError(Event<?> event, String message) {
		super(event, message);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param event the event that was being handled when the problem occurred
	 * @param message the message
	 * @param throwable the encountered throwable
	 */
	public IOError(Event<?> event, String message, Throwable throwable) {
		super(event, message, throwable);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param event the event that was being handled when the problem occurred
	 * @param throwable the encountered throwable
	 */
	public IOError(Event<?> event, Throwable throwable) {
		super(event, throwable);
	}

}
