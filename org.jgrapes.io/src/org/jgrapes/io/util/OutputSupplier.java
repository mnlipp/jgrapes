/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
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

package org.jgrapes.io.util;

import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;
import org.jgrapes.net.events.ClientConnected;

/**
 * May be used to associate (pending) {@link Output} events with another
 * event. As an example consider readily available data (not to be produced
 * lazily) that is to be emitted once a connection to a receiver has been
 * established. In this case the {@link OpenSocketConnection} event may 
 * be associated with an output supplier. The handler for the 
 * {@link ClientConnected} event can then check if the 
 * {@link OpenSocketConnection} event has an associated output supplier 
 * and call the supplier's {@link OutputSupplier#emit} method. 
 * 
 * @since 2.9.0
 */
@FunctionalInterface
public interface OutputSupplier {

    /**
     * Emit the {@link Output} events.
     *
     * @param channel the channel
     */
    void emit(IOSubchannel channel);

}
