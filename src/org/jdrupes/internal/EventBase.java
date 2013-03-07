/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes.internal;

import org.jdrupes.Channel;
import org.jdrupes.Event;

/**
 * @author mnl
 *
 */
public abstract class EventBase implements Matchable {

	/** The channels that this event is to be fired on if no
	 * channels are specified explicitly when firing. */
	protected Channel[] channels = null;
	/** The event that caused this event. */
	private EventBase generatedBy = null;
	/** Number of events that have to be dispatched until completion.
	 * This is one for the event itself and one more for each event
	 * that has this event as its cause. */
	private int openCount = 0;
	/** The event to be fired upon completion. */
	private Event completedEvent = null;
	
	/**
	 * Returns <code>true</code> if the event is currently being handled.
	 * 
	 * @return the result
	 */
	protected boolean currentlyHandled() {
		return openCount > 0;
	}
	
	/**
	 * Invoked when an exception occurs while invoking a handler for an event.
	 * 
	 * @param mgr the manager that has invoked the handler
	 * @param throwable the exception that has been thrown by the handler
	 */
	protected abstract void handlingError
		(EventManager mgr, Throwable throwable);
	
	/**
	 * @param generatedBy the causedBy to set
	 */
	void enqueued(EventBase generatedBy) {
		openCount += 1;
		this.generatedBy = generatedBy;
		if (generatedBy != null) {
			generatedBy.openCount += 1;
		}
	}

	/**
	 * @param mgr
	 */
	public void decrementOpen(EventManagerImpl mgr) {
		openCount -= 1;
		if (openCount == 0) {
			if (completedEvent != null) {
				Channel[] completeChannels = completedEvent.getChannels();
				if (completeChannels == null) {
					completeChannels = channels;
				}
				mgr.fire(completedEvent, completeChannels);
			}
			if (generatedBy != null) {
				generatedBy.decrementOpen(mgr);
			}
		}
	}

	/**
	 * @return the completedEvent
	 */
	public Event getCompletedEvent() {
		return completedEvent;
	}

	/**
	 * @param completedEvent the completedEvent to set
	 */
	public void setCompletedEvent(Event completedEvent) {
		this.completedEvent = completedEvent;
	}
}
