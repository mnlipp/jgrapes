/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Event;
import org.jgrapes.core.HandlerScope;

/**
 * This annotation tags some other annotation as a handler annotation. 
 * The tagged annotation can then be used to mark a method as a handler.
 *  
 * Every handler definition annotation must provide an {@link Evaluator} 
 * to allow access to the properties of the handler annotation in a 
 * uniform way. 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface HandlerDefinition {
	
	/**
	 * Returns the evaluator for the annotated handler annotation.
	 * 
	 * @return the evaluator
	 */
	Class<? extends Evaluator> evaluator();

	/**
	 * This interface allows access to the properties defined by arbitrary
	 * handler annotations in a uniform way. Handler annotations
	 * must specify the scope of a handler, i.e. for which events and
	 * channels the handler should be invoked, and the priority of
	 * the handler.  
	 */
	interface Evaluator {

		/**
		 * Returns the information about the events and channels handled
		 * by the handler that annotates the given method of the given
		 * comonent as a {@link HandlerScope} object. This method
		 * is invoked during object initialization. It may return
		 * {@code null} if a handler is not supposed to be added for
		 * this method during initialization (dynamic handler,
		 * see {@link Handler#dynamic()}). 
		 *
		 * @param component the component
		 * @param method the annotated method
		 * @param channelReplacements replacements for channel classes in 
		 * the annotation's `channels` element
		 * @return the scope or {@code null} if a handler for the method
		 * should not be created
		 */
		HandlerScope scope(ComponentType component, Method method, 
				ChannelReplacements channelReplacements);
		
		/**
		 * Returns the priority defined by the annotation
		 * 
		 * @param annotation the annotation
		 * @return the priority
		 */
		int priority(Annotation annotation);
		
		/**
		 * Utility method for checking if the method can be used as handler.
		 * 
		 * @param method the method
		 * @return the result
		 */
		static boolean checkMethodSignature(Method method) {
			return method.getParameterTypes().length == 0
			        || method.getParameterTypes().length == 1
			                && Event.class.isAssignableFrom(
			                        method.getParameterTypes()[0])
			        || (method.getParameterTypes().length == 2
			                && Event.class.isAssignableFrom(
			                        method.getParameterTypes()[0]))
			                && Channel.class.isAssignableFrom(
			                        method.getParameterTypes()[1]);
		}
	}

	/**
	 * Represents channel (criteria) replacements that are to
	 * be applied to `channels` elements of {@link Handler}
	 * annotations.
	 */
	@SuppressWarnings("serial")
	class ChannelReplacements 
		extends HashMap<Class<? extends Channel>, Object> {

		/**
		 * Create a new replacements specification object.
		 *
		 * @return the channel replacements
		 */
		public static ChannelReplacements create() {
			return new ChannelReplacements();
		}
		
		/**
		 * Adds a replacements to the resplacements.
		 *
		 * @param annotationCriterion the criterion used in the annotation
		 * @param replacement the replacement
		 * @return the channel replacements for easy chaining
		 */
		public ChannelReplacements add(Class<? extends Channel> annotationCriterion,
				Channel replacement) {
			put(annotationCriterion, replacement.defaultCriterion());
			return this;
		}
	}
}
