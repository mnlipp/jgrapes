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

package org.jgrapes.http.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler.NoChannel;
import org.jgrapes.core.annotation.Handler.NoEvent;
import org.jgrapes.core.annotation.HandlerDefinition;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.events.Request;

/**
 * This annotation marks a method as handler for events. The method is 
 * invoked for events that have a type derived from {@link Request} and
 * are matched by one of the specified {@link ResourcePattern}s.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
@HandlerDefinition(evaluator=RequestHandler.Evaluator.class)
public @interface RequestHandler {
	
	/**
	 * Specifies classes of events that the handler is to receive.
	 * 
	 * @return the event classes
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Event>[] events() default NoEvent.class;
	
	/**
	 * Specifies classes of channels that the handler listens on.
	 * 
	 * @return the channel classes
	 */
	Class<? extends Channel>[] channels() default NoChannel.class;

	/**
	 * Specifies the patterns that the handler is supposed to handle
	 * (see {@link ResourcePattern}).
	 * 
	 * @return the patterns
	 */
	String[] patterns() default "";

	/**
	 * Specifies a priority. The value is used to sort handlers.
	 * Handlers with higher priority are invoked first.
	 * 
	 * @return the priority
	 */
	int priority() default 0;

	/**
	 * Returns {@code true} if the annotated annotation defines a
	 * dynamic handler. A dynamic handler must be added to the set of
	 * handlers of a component explicitly.
	 * 
	 * @return the result
	 */
	boolean dynamic() default false;
	
	/**
	 * This class provides the {@link Evaluator} for the {@link RequestHandler}
	 * annotation. It implements the behavior as described for the annotation.
	 */
	public static class Evaluator implements HandlerDefinition.Evaluator {

		/* (non-Javadoc)
		 * @see org.jgrapes.core.annotation.HandlerDefinition.Evaluator#getPriority()
		 */
		@Override
		public int priority(Annotation annotation) {
			return ((RequestHandler)annotation).priority();
		}

		@Override
		public HandlerScope scope(
				ComponentType component, Method method, 
				Object[] eventValues, Object[] channelValues) {
			RequestHandler annotation 
				= method.getAnnotation(RequestHandler.class);
			if (annotation.dynamic()) {
				return null;
			}
			return new Scope(component, method, (RequestHandler)annotation,
					null);
		}

		/**
		 * Adds the given method of the given component as a 
		 * dynamic handler for a specific pattern. Other informations
		 * are taken from the annotation.
		 * 
		 * @param component the component
		 * @param method the name of the method that implements the handler
		 * @param pattern the pattern
		 */
		public static void add(ComponentType component, String method,
				String pattern) {
			try {
				for (Method m: component.getClass().getMethods()) {
					if (!m.getName().equals(method)) {
						continue;
					}
					for (Annotation annotation: m.getDeclaredAnnotations()) {
						Class<?> annoType = annotation.annotationType();
						HandlerDefinition hda 
							= annoType.getAnnotation(HandlerDefinition.class);
						if (hda == null
							|| !RequestHandler.class.isAssignableFrom(annoType)
							|| !((RequestHandler)annotation).dynamic()) {
							continue;
						}
						Scope scope = new Scope(component, m, 
								(RequestHandler)annotation, pattern);
						Components.manager(component)
							.addHandler(m, scope, 
									((RequestHandler)annotation).priority());
						return;
					}
				}
				throw new IllegalArgumentException(
						"No method named \"" + method + "\" with DynamicHandler"
						+ " annotation and correct parameter list.");
			} catch (SecurityException e) {
				throw (RuntimeException)
					(new IllegalArgumentException().initCause(e));
			}
		}
		
		public static class Scope implements HandlerScope {

			private Set<Object> handledEventTypes = new HashSet<>();
			private Set<Object> handledChannels = new HashSet<>();
			private Set<ResourcePattern> handledPatterns = new HashSet<>();

			public Scope(ComponentType component, 
					Method method, RequestHandler annotation, String pattern) {
				if (!HandlerDefinition.Evaluator.checkMethodSignature(method)) {
					throw new IllegalArgumentException("Method "
							 + method.toString() + " cannot be used as"
							 + " handler (wrong signature).");
				}
				// Get all event keys from the handler annotation.
				if (annotation.events()[0] != NoEvent.class) {
					handledEventTypes.addAll(Arrays.asList(annotation.events()));
				}
				// If no event types are given, try first parameter.
				if (handledEventTypes.isEmpty()) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length > 0) {
						if (Event.class.isAssignableFrom(paramTypes[0])) {
							handledEventTypes.add(paramTypes[0]);
						}
					}
				}
				
				// Get channel keys from the annotation.
				boolean addDefaultChannel = false;
				if (annotation.channels()[0] != NoChannel.class) {
					for (Class<?> c: annotation.channels()) {
						if (c == Self.class) {
							if (!(component instanceof Channel)) {
								throw new IllegalArgumentException(
								    "Canot use channel This.class in "
									+ "annotation of " + method 
									+ " because " + getClass().getName()
								    + " does not implement Channel.");
							}
							// Will be added anyway, see below
						} else if (c == Channel.Default.class) {
							addDefaultChannel = true;
						} else {
							handledChannels.add(c);
						}
					}
				}
				if (handledChannels.size() == 0 || addDefaultChannel) {
					handledChannels.add(Components.manager(component)
					        .channel().defaultCriterion());
				}
				// Finally, a comoponent always handles events
				// directed at it directly.
				if (component instanceof Channel) {
					handledChannels.add(
							((Channel) component).defaultCriterion());
				}
				
				try {
				// Get all paths from the annotation.
					if (!annotation.patterns()[0].equals("")) {
						for (String p: annotation.patterns()) {
							handledPatterns.add(new ResourcePattern(p));
						}
					}
				
					// Add additionally provided path
					if (pattern != null) {
						handledPatterns.add(new ResourcePattern(pattern));
					}
				} catch (ParseException e) {
					throw new IllegalArgumentException(e.getMessage());
				}
			}
			
			@Override
			public boolean includes(Eligible event, Eligible[] channels) {
				boolean match = false;
				for (Object eventType: handledEventTypes) {
					if (Class.class.isInstance(eventType)
					        && ((Class<?>) eventType)
					                .isAssignableFrom(event.getClass())) {
						match = true;
						break;
					}
					
				}
				if (!match) {
					return false;
				}
				match = false;
				// Try channels
				for (Eligible channel: channels) {
					for (Object channelValue: handledChannels) {
						if (channel.isEligibleFor(channelValue)) {
							match = true;
							break;
						}
					}
				}
				if (!match) {
					return false;
				}
				if (!(event instanceof Request)) {
					return false;
				}
				for (ResourcePattern rp: handledPatterns) {
					if (((Request)event).isEligibleFor(
							Request.createMatchValue(Request.class, rp))) {
						return true;
					}
				}
				return false;
			}

			/* (non-Javadoc)
			 * @see java.lang.Object#toString()
			 */
			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				builder.append("Scope [");
				if (handledEventTypes != null) {
					builder.append("handledEventTypes=");
					builder.append(handledEventTypes.stream().map(v -> {
						if (v instanceof Class) {
							return Components.className((Class<?>) v);
						}
						return v.toString();
					}).collect(Collectors.toSet()));
					builder.append(", ");
				}
				if (handledChannels != null) {
					builder.append("handledChannels=");
					builder.append(handledChannels);
					builder.append(", ");
				}
				if (handledPatterns != null) {
					builder.append("handledPatterns=");
					builder.append(handledPatterns);
				}
				builder.append("]");
				return builder.toString();
			}
			
		}
	}
}