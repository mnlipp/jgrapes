/*
 * JGrapes Event driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail.events;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import java.util.List;
import java.util.Map;
import org.jgrapes.core.Event;
import org.jgrapes.mail.MailChannel;
import org.jgrapes.mail.MailStoreMonitor;

/**
 * Signals the retrieval of mails (update) by a {@link MailStoreMonitor}.
 * Must be fired on a {@link MailChannel}.
 */
public class MessagesRetrieved extends Event<Void> {

    private final Map<String, List<Message>> allMessages;
    private final List<Message> newMessages;

    /**
     * Instantiates a new event.
     *
     * @param allMessages the messages
     */
    public MessagesRetrieved(Map<String, List<Message>> allMessages,
            List<Message> newMessages) {
        this.allMessages = allMessages;
        this.newMessages = newMessages;
    }

    /**
     * Return all messages retrieved.
     *
     * @return the map
     */
    public Map<String, List<Message>> allMessages() {
        return allMessages;
    }

    /**
     * Return the new messages. New messages have not been reported
     * before by an event.
     *
     * @return the list
     */
    public List<Message> newMessages() {
        return newMessages;
    }
}
