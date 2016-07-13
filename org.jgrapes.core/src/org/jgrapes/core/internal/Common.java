/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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
package org.jgrapes.core.internal;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentNode;

/**
 * @author Michael N. Lipp
 *
 */
public class Common {

	private Common() {
	}

	private static AssertionError assertionError = null;
	
	static void setAssertionError (AssertionError error) {
		if (assertionError == null) {
			assertionError = error;
		}
	}

	static public void checkAssertions() {
		if (assertionError != null) {
			AssertionError error = assertionError;
			assertionError = null;
			throw error;
		}
	}
	
	public static final Logger classNames 
		= Logger.getLogger(ComponentNode.class.getPackage().getName() 
			+ ".classNames");	

	public static String classToString(Class<?> clazz) {
		if (classNames.isLoggable(Level.FINER)) {
			return clazz.getName();
		} else {
			return clazz.getSimpleName();
		}
	}

	public static String channelsToString(Channel[] channels) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		boolean first = true;
		for (Channel c: channels) {
			if (!first) {
				builder.append(", ");
			}
			builder.append(channelKeyToString(c.getMatchKey()));
			first = false;
		}
		builder.append("]");
		return builder.toString();
	}

	
	public static String channelKeyToString(Object channelKey) {
		StringBuilder builder = new StringBuilder();
		if (channelKey instanceof Class) {
			if (channelKey == Channel.class) {
				builder.append("BROADCAST");
			} else {
				builder.append(Common.classToString((Class<?>) channelKey));
			}
		} else {
			builder.append(channelKey);
		}
		return builder.toString();
	}
	
	private static Map<Object, String> objectIds = new WeakHashMap<>();
	private static Map<Class<?>, AtomicLong> idCounters = new WeakHashMap<>();

	public static String getId(Class<?> clazz, Object object) {
		if (object == null) {
			return "?";
		}
		synchronized (objectIds) {
			return objectIds.computeIfAbsent
				(object, k -> Long.toString
					(idCounters.computeIfAbsent(clazz, l -> new AtomicLong())
							.incrementAndGet()));
			
		}
	}

}
