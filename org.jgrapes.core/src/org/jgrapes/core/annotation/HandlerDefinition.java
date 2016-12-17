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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.Manager;

/**
 * This annotation marks some other annotation (a.k.a the handler annotation) 
 * as an annotation that can be used to mark a method as a handler. 
 * The annotation must specify an {@link Evaluator} that can be used to access
 * the properties of the handler annotation in a uniform way. 
 * 
 * @author Michael N. Lipp
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.ANNOTATION_TYPE)
public @interface HandlerDefinition {
	
	/**
	 * Returns the evaluator for the annotated handler annotation.
	 * 
	 * @return the evaluator
	 */
	Class<? extends Evaluator> evaluator();

	/**
	 * Returns {@code true} if the annotated annotation defines a
	 * dynamic handler. A dynamic handler must be added to the set of
	 * handlers of a component explicitly.
	 * 
	 * @return the result
	 */
	boolean dynamic() default false;
	
	/**
	 * This interface allows access to the properties defined by
	 * any handler annotation in a uniform way. Handler annotations
	 * must specify the scope of a handler, i.e. for which events and
	 * channels the handler should be invoked, and the priority of
	 * the handler.  
	 * 
	 * @author Michael N. Lipp
	 */
	public interface Evaluator {
		
		/**
		 * Returns the priority defined by the annotation
		 * 
		 * @param annotation the annotation
		 * @return the priority
		 */
		int getPriority(Annotation annotation);

		/**
		 * Returns the information about the events and channels handled
		 * by the handler as a {@link HandlerScope} object.
		 * 
		 * @param annotation the annotation
		 * @param component the component
		 * @param method the annotated method
		 * @param eventValues event values that can be used in addition
		 * to or as replacements for the values specified in the
		 * annotation
		 * @param channelValues channel values that can be used in addition
		 * to or as replacements for the values specified in the
		 * annotation
		 * @return the result
		 */
		HandlerScope getScope
			(Annotation annotation, Manager component, Method method, 
					Object[] eventValues, Object[] channelValues);
	}

}
