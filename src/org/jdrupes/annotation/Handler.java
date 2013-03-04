/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdrupes.Channel;
import org.jdrupes.ClassChannel;
import org.jdrupes.Event;
import org.jdrupes.NamedChannel;
import org.jdrupes.NamedEvent;

/**
 * Marks a method as handler for events. The method is invoked for
 * events that have a type (or name) matching the given events
 * (or namedEvents) parameter and that are fired on the given
 * channels (or namedChannels). 
 * 
 * @author mnl
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface Handler {
	
	/** Default value for the <code>events</code> parameter of
	 * the annotation. Indicates that the parameter is not used. */
	final public static class NO_EVENT extends Event {
	}
	
	/** Default value for the <code>channels</code> parameter of
	 * the annotation. Indicates that the parameter is not used. */
	final public static class NO_CHANNEL extends ClassChannel {
	}
	
	/**
	 * The <code>events</code> parameter specifies classes of events 
	 * that the handler is to receive.
	 */
	Class<? extends Event>[] events() default NO_EVENT.class;
	
	/**
	 * The <code>namedEvents</code> parameter specifies names of  
	 * {@link NamedEvent}s that the handler is to receive.
	 */
	String[] namedEvents() default "";
	
	/**
	 * The <code>channels</code> parameter specifies classes of channels 
	 * that the handler listens on.
	 */
	Class<? extends Channel>[] channels() default NO_CHANNEL.class;

	/**
	 * The <code>namedChannels</code> parameter specifies names of  
	 * {@link NamedChannel}s that the handler listens on.
	 */
	String[] namedChannels() default "";
}
