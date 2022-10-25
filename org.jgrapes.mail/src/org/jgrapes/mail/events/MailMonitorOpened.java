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

import jakarta.mail.Store;
import org.jgrapes.io.events.Opened;

/**
 * Fired when the requested connection has been established.
 */
public class MailMonitorOpened extends Opened<OpenMailMonitor> {

    private final Store store;

    /**
     * Instantiates a new mail monitor opened.
     *
     * @param openEvent the open event
     */
    public MailMonitorOpened(OpenMailMonitor openEvent, Store store) {
        this.store = store;
        setResult(openEvent);
    }

    /**
     * Returns the event that caused this connection to be established.
     * 
     * @return the event
     */
    public OpenMailMonitor openEvent() {
        return currentResults().get(0);
    }

    /**
     * Returns the store that has been opened.
     *
     * @return the store
     */
    public Store getStore() {
        return store;
    }
}
