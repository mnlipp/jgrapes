/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017 Michael N. Lipp
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

package org.jgrapes.http;

import java.util.Map;
import java.util.function.Function;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentCollector;
import org.jgrapes.core.ComponentType;

/**
 * A component that collects all services of type
 * {@link HttpRequestHandlerFactory} and creates an
 * instance of each.
 */
public class HttpRequestHandlerCollector 
	extends ComponentCollector<HttpRequestHandlerFactory> {

	public HttpRequestHandlerCollector(Channel componentChannel,
			Function<Class<? extends ComponentType>,Map<Object,Object>> matcher) {
		super(HttpRequestHandlerFactory.class, componentChannel, matcher);
	}

}
