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
package org.jgrapes.core.internal;

import java.util.ArrayList;

import org.jgrapes.core.EventPipeline;

/**
 * A list of handlers for a given event type and set of channels.
 * 
 * @author Michael N. Lipp
 */
@SuppressWarnings("serial")
class HandlerList extends ArrayList<HandlerReference> {

	/**
	 * Invoke all handlers with the given event as parameter.
	 * 
	 * @param eventProcessor
	 * @param event the event
	 */
	public void process(EventPipeline eventPipeline, EventBase event) {
		for (HandlerReference hdlr: this) {
			try {
				hdlr.invoke(event);
				if (event.isStopped()) {
					break;
				}
			} catch (Throwable t) {
				event.handlingError(eventPipeline, t);
			}
		}
		event.stop();
		try {
			event.stopped();
		} catch (Throwable t) {
			event.handlingError(eventPipeline, t);
		}
	}

}
