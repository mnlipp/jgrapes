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

package org.jgrapes.http.events;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.http.Session;

/**
 * Causes a session manager to dicard the given session.
 */
public class DiscardSession extends Event<Void> {

    private final Session session;

    /**
     * Creates a new event.
     * 
     * @param session the session
     * @param channels
     */
    public DiscardSession(Session session, Channel... channels) {
        super(channels);
        this.session = session;
    }

    /**
     * Returns the session to be discarded.
     * 
     * @return the session
     */
    public Session session() {
        return session;
    }
}
