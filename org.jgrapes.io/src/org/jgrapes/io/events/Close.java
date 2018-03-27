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

import org.jgrapes.core.Event;

/**
 * This event causes the initiator of an I/O channel to shutdown the
 * channel. After terminating any outstanding actions, the initiator
 * must eventually send a {@link Closed} event on the I/O channel.
 * 
 * The {@link Closed} need not be generated as a direct response to
 * the {@link Close} event. If the initiator is a server and the
 * network protocol supports this, the server may first send a close
 * message to the client and fire the {@link Closed} event when the
 * confirmation is received from the client. 
 */
public class Close extends Event<Void> {

	/**
	 * Creates a new event.
	 */
	public Close() {
	}

}
