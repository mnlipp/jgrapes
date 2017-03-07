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

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler.NoChannel;

/**
 * This annotation marks a component's attribute of type 
 * {@link Manager} as a slot for its manager. A value is automatically 
 * assigned to such an attribute when a component is attached to the 
 * component tree or by {@link org.jgrapes.core.Components#manager(ComponentType)}.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD)
public @interface ComponentManager {
	
	/**
	 * Specifies the channel to be associated with the component
	 * as a {@link ClassChannel}'s key.
	 * 
	 * @return the channel
	 */
	Class<? extends Channel> channel() default NoChannel.class;

	/**
	 * Specifies the channel to be associated with the component
	 * as a {@link org.jgrapes.core.NamedChannel}'s key (a <code>String</code>).
	 * 
	 * @return the channel
	 */
	String namedChannel() default "";
}