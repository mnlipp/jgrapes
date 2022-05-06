/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.io.events;

import org.jgrapes.core.Event;
import org.jgrapes.io.IOSubchannel;

/**
 * This event is fired on {@link IOSubchannel}s that are created
 * by provider components before their first usage for {@link Input}
 * or {@link Output} events.
 * 
 * The subsequent {@link Opened} event (or some specialization) will
 * be sent only after the {@link Opening} event has completed. This
 * allows components to set up additional data processing for the newly
 * created {@link IOSubchannel} before data is sent to it.
 */
public class Opening<T> extends Event<T> {

}
