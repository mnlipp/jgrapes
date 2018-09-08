/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.io;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Purge;

/**
 * A component that handles {@link Purge} events
 * by unconditionally firing a {@link Close} events as response.
 */
public class PurgeTerminator extends Component {

    /**
     * @param componentChannel
     */
    public PurgeTerminator(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Handles a {@link Purge} event by sending a {@link Close} event.
     *
     * @param event the event
     * @param channel the channel
     */
    @Handler
    public void onPurge(Purge event, IOSubchannel channel) {
        // Needn't close this more than once
        event.stop();
        channel.respond(new Close());
    }
}
