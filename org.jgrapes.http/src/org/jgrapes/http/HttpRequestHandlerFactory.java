/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

package org.jgrapes.http;

import org.jgrapes.core.ComponentFactory;
import org.jgrapes.http.events.Request;

/**
 * A component factory that creates components with at least one
 * handler for {@link Request} events. The kind of requests and
 * the service(s) provided are up to the component. If created using 
 * {@link ComponentFactory#create(org.jgrapes.core.Channel, java.util.Map)},
 * the created component may only handle requests that start with the
 * specified prefix.
 */
public interface HttpRequestHandlerFactory extends ComponentFactory {

    /** The key for the prefix property. */
    @SuppressWarnings("PMD.ConstantsInInterface")
    String PREFIX = HttpRequestHandlerFactory.class.getName()
        + ".PREFIX";
}
