/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.mail.events;

import jakarta.mail.Message;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 * Indicates the arrival of a new message. Handler should delete
 * the message after successful processing.
 */
public class ReceivedMailMessage extends Event<Void> {

    private final Message message;

    /**
     * Creates a new event.
     *
     * @param message the message
     * @param channels the channels
     */
    public ReceivedMailMessage(Message message, Channel... channels) {
        super(channels);
        this.message = message;
    }

    /**
     * Returns the message.
     *
     * @return the message
     */
    public Message message() {
        return message;
    }

}
