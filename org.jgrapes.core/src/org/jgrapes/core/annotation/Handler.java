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

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.DefaultChannel;
import org.jgrapes.core.Event;
import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Criterion;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.Self;

/**
 * This annotation marks a method as handler for events. The method is 
 * invoked for events that have a type (or name) matching the given events
 * (or namedEvents) parameter and that are fired on the given
 * channels (or namedChannels). 
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
	final public static class NO_EVENT extends Event<Void> {
	}
	
	/** The default value for the <code>channels</code> parameter of
	 * the annotation. Indicates that the parameter is not used. */
	final public static class NO_CHANNEL extends ClassChannel {
	}
	
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
	 * Specifies classes of channels that the handler listens on.
	 * 
	 * @return the channel classes
	 */
	Class<? extends Channel>[] channels() default NO_CHANNEL.class;

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
	 * This class provides the {@link Evaluator} for the (default)
	 * {@link Handler} annotation provided by the core package. It 
	 * implements the behavior as described for the annotation. 
	 * 
	 * @author Michael N. Lipp
	 */
	public static class Evaluator implements HandlerDefinition.Evaluator {

		/* (non-Javadoc)
		 * @see org.jgrapes.core.annotation.HandlerDefinition.Evaluator#getPriority()
		 */
		@Override
		public int getPriority(Annotation annotation) {
			return ((Handler)annotation).priority();
		}

		@Override
		public HandlerScope getScope
			(Annotation annotation, Manager component, Method method, 
					Object[] eventValues, Object[] channelValues) {
			return new Scope(component, method, (Handler)annotation);
		}
		
		private static class Scope implements HandlerScope {

			private Set<Object> handledEvents = new HashSet<Object>();
			private Set<Object> handledChannels = new HashSet<Object>();

			public Scope(Manager component, 
					Method method, Handler annotation) {
				// Get all event keys from the handler annotation.
				if (annotation.events()[0] != Handler.NO_EVENT.class) {
					handledEvents.addAll(Arrays.asList(annotation.events()));
				}
				// Get all named events from the annotation and add to event keys.
				if (!annotation.namedEvents()[0].equals("")) {
					handledEvents.addAll(Arrays.asList(annotation.namedEvents()));
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
				
				// Get channel keys from the annotation.
				boolean addDefaultChannel = false;
				if (annotation.channels()[0] != Handler.NO_CHANNEL.class) {
					for (Class<?> c: annotation.channels()) {
						if (c == Self.class) {
							if (component instanceof Channel) {
								handledChannels
									.add(((Channel)component).getMatchValue());
							} else {
								throw new IllegalArgumentException
									("Canot use channel This.class in annotation"
									 + " of " + method + " because " 
									 + getClass().getName() 
									 + " does not implement Channel.");
							}
						} else if (c == DefaultChannel.class) {
							addDefaultChannel = true;
						} else {
							handledChannels.add(c);
						}
					}
				}
				// Get named channels from annotation and add to channel keys.
				if (!annotation.namedChannels()[0].equals("")) {
					handledChannels.addAll
						(Arrays.asList(annotation.namedChannels()));
				}
				if (handledChannels.size() == 0 || addDefaultChannel) {
					handledChannels.add(component.getChannel().getMatchValue());
				}
			}
			
			@Override
			public boolean includes(Criterion event, Criterion[] channels) {
				for (Object eventValue: handledEvents) {
					if (event.isMatchedBy(eventValue)) {
						// Found match regarding event, now try channels
						for (Criterion channel: channels) {
							for (Object channelValue: handledChannels) {
								if (channel.isMatchedBy(channelValue)) {
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
					builder.append(handledEvents);
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
