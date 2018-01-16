/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.core;

import org.jgrapes.core.internal.CompletionLockBase;
import org.jgrapes.core.internal.EventBase;

/**
 * Represents a lock that prevents sending completion events. Completion
 * locks can be added to events. They have the same effect as an event 
 * that has been fired during the execution and that has not been processed
 * yet.
 * 
 * Removing the last completion lock from an event after all events
 * fired by its handlers have been processed will cause the completion
 * events to be fired. 
 * 
 * @see EventBase#addCompletionLock
 */
public class CompletionLock extends CompletionLockBase {

	/**
	 * Creates a completion lock for the given event with the given timeout. 
	 * 
	 * @param event the event to be locked
	 * @param timeout the timeout, if zero, the lock will be held up until
	 * it is released
	 */
	public CompletionLock(Event<?> event, long timeout) {
		super(event, timeout);
	}
	
	/**
	 * Creates a completion lock without timeout.
	 * 
	 * @param event the event to be locked
	 */
	public CompletionLock(Event<?> event) {
		super(event, 0);
	}
	
}
