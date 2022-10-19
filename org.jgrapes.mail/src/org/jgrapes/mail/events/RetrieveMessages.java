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
 * Triggers the retrieval of mails (update) by a {@link MailStoreMonitor}.
 * Must be fired on a {@link MailChannel}.
 */
public class RetrieveMessages extends Event<Map<Folder, List<Message>>> {

    private final String[] folderNames;

    /**
     * Causes new messages to be retrieved from the given folders.
     *
     * @param folderNames the folder names
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public RetrieveMessages(String... folderNames) {
        this.folderNames = folderNames;
        new MessagesRetrieved(this);
    }

    /**
     * Returns the folder names.
     *
     * @return the string[]
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public String[] folderNames() {
        return folderNames;
    }

}
