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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.DefaultChannel;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.Self;
import org.jgrapes.core.internal.Common;

/**
 * This is the basic, general purpose handler annotation provided as part of the
 * core package. The annotated method is invoked for events that have a type (or
 * name) matching the given events (or namedEvents) parameter and that are fired
 * on the given channels (or namedChannels).
 * 
 * @author Michael N. Lipp
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
@HandlerDefinition(evaluator=Handler.Evaluator.class)
public @interface Handler {
	
	/** The default value for the <code>events</code> parameter of
	 * the annotation. Indicates that the parameter is not used. */
	public static final class NoEvent extends Event<Void> {
	}
	
	/** The default value for the <code>channels</code> parameter of
	 * the annotation. Indicates that the parameter is not used. */
	public static final class NoChannel extends ClassChannel {
	}
	
	/**
	 * Specifies classes of events that the handler is to receive.
	 * 
	 * @return the event classes
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Event>[] events() default NoEvent.class;
	
	/**
	 * Specifies names of {@link NamedEvent}s that the handler is to receive.
	 * 
	 * @return the event names
	 */
	String[] namedEvents() default "";
	
	/**
	 * Specifies classes of channels that the handler listens on.
	 * 
	 * @return the channel classes
	 */
	Class<? extends Channel>[] channels() default NoChannel.class;

	/**
	 * Specifies names of {@link NamedChannel}s that the handler listens on.
	 * 
	 * @return the channel names
	 */
	String[] namedChannels() default "";
	
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
	 * This class provides the {@link Evaluator} for the 
	 * {@link Handler} annotation provided by the core package. It 
	 * implements the behavior as described for the annotation. 
	 * 
	 * @author Michael N. Lipp
	 */
	public static class Evaluator implements HandlerDefinition.Evaluator {

		@Override
		public HandlerScope getScope(
				ComponentType component, Method method, 
					Object[] eventValues, Object[] channelValues) {
			Handler annotation = method.getAnnotation(Handler.class);
			if (annotation == null || annotation.dynamic()) {
				return null;
			}
			return new Scope(component, method, annotation, eventValues,
			        channelValues);
		}
		
		/* (non-Javadoc)
		 * @see org.jgrapes.core.annotation.HandlerDefinition.Evaluator#getPriority()
		 */
		@Override
		public int getPriority(Annotation annotation) {
			return ((Handler)annotation).priority();
		}

		/**
		 * Adds the given method of the given component as a dynamic handler for
		 * a specific event and channel. The method with the given name must be
		 * annotated as dynamic handler and must have a single argument of type
		 * {@link Event} (or a derived type as appropriate for the event type to
		 * be handled).
		 * 
		 * @param component
		 *            the component
		 * @param method
		 *            the name of the method that implements the handler
		 * @param eventValue
		 *            the event key that should be used for matching this
		 *            handler with an event. This is equivalent to an
		 *            <code>events</code>/<code>namedEvents</code> parameter
		 *            used with a single value in the handler annotation, but
		 *            here all kinds of Objects are allowed as key values.
		 * @param channelValue
		 *            the channel key that should be used for matching this
		 *            handler with a channel. This is equivalent to a
		 *            <code>channels</code>/<code>namedChannels</code> parameter
		 *            used with a single value in the handler annotation, but
		 *            here all kinds of Objects are allowed as key values. As a
		 *            convenience, if the actual object provided is a
		 *            {@link Channel}, its match value is used for matching.
		 * @param priority
		 *            the priority of the handler
		 */
		public static void add(ComponentType component, String method,
				Object eventValue, Object channelValue, int priority) {
			try {
				if (channelValue instanceof Channel) {
					channelValue = ((Eligible)channelValue).getDefaultCriterion();
				}
				for (Method m: component.getClass().getMethods()) {
					if (!m.getName().equals(method)) {
						continue;
					}
					if (m.getParameterTypes().length != 0
							&& !(m.getParameterTypes().length == 1
								 && Event.class.isAssignableFrom(
										 m.getParameterTypes()[0]))) {
						continue;
					}
					for (Annotation annotation: m.getDeclaredAnnotations()) {
						Class<?> annoType = annotation.annotationType();
						HandlerDefinition hda 
							= annoType.getAnnotation(HandlerDefinition.class);
						if (hda == null
								|| !Handler.class.isAssignableFrom(annoType)
								|| !((Handler)annotation).dynamic()) {
							continue;
						}
						Scope scope = new Scope(component, m, 
								(Handler)annotation,
								new Object[] { eventValue },
								new Object[] { channelValue });
						Components.manager(component)
							.addHandler(m, scope, priority);
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
		
		/**
		 * Add a handler like 
		 * {@link #add(ComponentType, String, Object, Object, int)}
		 * but take the values for event and priority from the annotation.
		 * 
		 * @param component the component
		 * @param method the name of the method that implements the handler
		 * @param channelValue the channel key that should be used for matching
		 * this handler with a channel 
		 */
		public static void add(ComponentType component, String method,
		        Object channelValue) {
			try {
				if (channelValue instanceof Channel) {
					channelValue = ((Eligible)channelValue).getDefaultCriterion();
				}
				for (Method m: component.getClass().getMethods()) {
					if (!m.getName().equals(method)) {
						continue;
					}
					if (m.getParameterTypes().length != 0
							&& !(m.getParameterTypes().length == 1
								 && Event.class.isAssignableFrom(
										 m.getParameterTypes()[0]))) {
						continue;
					}
					for (Annotation annotation: m.getDeclaredAnnotations()) {
						Class<?> annoType = annotation.annotationType();
						if (!(annoType.equals(Handler.class))) {
							continue;
						}
						HandlerDefinition hda 
							= annoType.getAnnotation(HandlerDefinition.class);
						if (hda == null
								|| !Handler.class.isAssignableFrom(annoType)
								|| !((Handler)annotation).dynamic()) {
							continue;
						}
						Scope scope = new Scope(component, m,
						        (Handler) annotation, null,
						        new Object[] { channelValue });
						Components.manager(component).addHandler(m, scope, 
								((Handler) annotation).priority());
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
		
		/**
		 * The handler scope implementation used by the evaluator.
		 * 
		 * @author Michael N. Lipp
		 */
		private static class Scope implements HandlerScope {

			private Set<Object> handledEvents = new HashSet<Object>();
			private Set<Object> handledChannels = new HashSet<Object>();

			public Scope(ComponentType component, Method method,
			        Handler annotation, Object[] eventValues,
			        Object[] channelValues) {
				if (eventValues != null) {
					handledEvents.addAll(Arrays.asList(eventValues));
				} else {
					// Get all event values from the handler annotation.
					if (annotation.events()[0] != Handler.NoEvent.class) {
						handledEvents
						        .addAll(Arrays.asList(annotation.events()));
					}
					// Get all named events from the annotation and add to event
					// keys.
					if (!annotation.namedEvents()[0].equals("")) {
						handledEvents.addAll(
						        Arrays.asList(annotation.namedEvents()));
					}
					// If no event types are given, try first parameter.
					if (handledEvents.isEmpty()) {
						Class<?>[] paramTypes = method.getParameterTypes();
						if (paramTypes.length > 0) {
							if (Event.class.isAssignableFrom(paramTypes[0])) {
								handledEvents.add(paramTypes[0]);
							}
						}
					}
				}
				
				if (channelValues != null) {
					handledChannels.addAll(Arrays.asList(channelValues));
				} else {
					// Get channel values from the annotation.
					boolean addDefaultChannel = false;
					if (annotation.channels()[0] != Handler.NoChannel.class) {
						for (Class<?> c : annotation.channels()) {
							if (c == Self.class) {
								if (component instanceof Channel) {
									handledChannels
									        .add(((Channel) component)
									                .getDefaultCriterion());
								} else {
									throw new IllegalArgumentException(
									    "Canot use channel This.class in "
										+ "annotation of " + method 
										+ " because " + getClass().getName()
									    + " does not implement Channel.");
								}
							} else if (c == DefaultChannel.class) {
								addDefaultChannel = true;
							} else {
								handledChannels.add(c);
							}
						}
					}
					// Get named channels from annotation and add to channel
					// keys.
					if (!annotation.namedChannels()[0].equals("")) {
						handledChannels.addAll(
						        Arrays.asList(annotation.namedChannels()));
					}
					if (handledChannels.size() == 0 || addDefaultChannel) {
						handledChannels.add(Components.manager(component)
						        .getChannel().getDefaultCriterion());
					}
				}
			}
			
			@Override
			public boolean includes(Eligible event, Eligible[] channels) {
				for (Object eventValue: handledEvents) {
					if (event.isEligibleFor(eventValue)) {
						// Found match regarding event, now try channels
						for (Eligible channel: channels) {
							for (Object channelValue: handledChannels) {
								if (channel.isEligibleFor(channelValue)) {
									return true;
								}
							}
						}
						return false;
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
				if (handledEvents != null) {
					builder.append("handledEvents=");
					builder.append(handledEvents.stream().map(v -> {
						if (v instanceof Class) {
							return Common.classToString((Class<?>) v);
						}
						return v.toString();
					}).collect(Collectors.toSet()));
					builder.append(", ");
				}
				if (handledChannels != null) {
					builder.append("handledChannels=");
					builder.append(handledChannels);
				}
				builder.append("]");
				return builder.toString();
			}
			
		}
	}
}
