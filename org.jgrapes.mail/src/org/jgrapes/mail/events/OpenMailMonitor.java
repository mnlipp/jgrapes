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

import java.util.Arrays;

/**
 * An event that opens folders on the store defined by the
 * base class for monitoring.
 */
public class OpenMailMonitor extends OpenMailConnection {

    private String[] folderNames = { "INBOX" };

    /**
     * Creates a new monitor connection, watching the specified folders.
     *
     * @param folderNames the folders, defaults to "INBOX"
     * @return the open mail monitor
     */
    public OpenMailMonitor(String... folderNames) {
        if (folderNames.length > 0) {
            this.folderNames = Arrays.copyOf(folderNames, folderNames.length);
        }
    }

    /**
     * Returns the watched folders.
     *
     * @return the string[]
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public String[] folderNames() {
        return folderNames;
    }

}
