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
package org.jgrapes.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.annotation.Handler.NO_EVENT;

/**
 * Marks a method as dynamic handler. Dynamic handlers can be added
 * explicitly to the set of handlers by the programmer. This is usually
 * required if the channels to listen on for events aren't known at 
 * compile time.
 * 
 * @author Michael N. Lipp
 * @see Manager#addHandler(String, Object, Object, int)
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface DynamicHandler {
	
	/**
	 * Specifies classes of events that the handler is to receive.
	 * 
	 * @return the event classes
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Event>[] events() default NO_EVENT.class;
	
	/**
	 * Specifies names of {@link NamedEvent}s that the handler is to receive.
	 * 
	 * @return the event names
	 */
	String[] namedEvents() default "";
	
	/**
	 * Specifies a priority. The value is used to sort handlers.
	 * Handlers with higher priority are invoked first.
	 * 
	 * @return the priority
	 */
	int priority() default 0;
}
