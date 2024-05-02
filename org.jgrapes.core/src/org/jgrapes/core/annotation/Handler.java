/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Channel.Default;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Eligible;
import org.jgrapes.core.Event;
import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.Manager;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;

/**
 * This is the basic, general purpose handler annotation provided as part of the
 * core package.
 * 
 * The annotated method is invoked for events that have a type (or
 * name) matching the given `events` (or `namedEvents`) element 
 * of the annotation and that are fired on one of
 * the `channels` (or `namedChannels`) specified in the annotation.
 * 
 * If neither event classes nor named events are specified in the 
 * annotation, the class of the annotated method's first parameter (which 
 * must be of type {@link Event} or a derived type) is used as (single)
 * event class (see the examples in {@link #events()} and 
 * {@link #namedEvents()}).
 * 
 * Channel matching is performed by matching the event's channels
 * (see {@link Event#channels()}) with the channels specified in the
 * handler. The matching algorithm invokes
 * {@link Eligible#isEligibleFor(Object) isEligibleFor} for each of the 
 * event's channels with the class (or name, see {@link #channels()} and 
 * {@link Handler#namedChannels()}) of each of the channels specified
 * in the handler.
 * 
 * If neither channel classes not named channels are specified in the 
 * handler, or `{@link Default Channel.Default}.class` is specified as one 
 * of the channel classes, the matching algorithm invokes
 * {@link Eligible#isEligibleFor(Object) isEligibleFor} for each of
 * the event's channels with the default criterion of the component's 
 * channel (see {@link Manager#channel()} and 
 * {@link Eligible#defaultCriterion()}) as argument.
 *
 * Finally, independent of any specified channels, the matching algorithm 
 * invokes {@link Eligible#isEligibleFor(Object) isEligibleFor} 
 * for each of the event's channels with the component's default criterion
 * as argument unless {@link #excludeSelf()} is set. This results in a match
 * if the component itself is used as one of the event's channels
 * (see the description of {@link Eligible}).
 * 
 * If a match is found for a given event's properties and a handler's
 * specified attributes, the handler method is invoked. 
 * The method can have an additional optional parameter of type
 * {@link Channel} (or a derived type). This parameter does not 
 * influence the eligibility of the method regarding a given event,
 * it determines how the method is invoked. If the method does not
 * have a second parameter, it is invoked once if an event 
 * matches. If the parameter exists, the method is invoked once for
 * each of the event's channels, provided that the optional parameter's
 * type is assignable from the event's channel.
 * 
 * Because annotation elements accept only literals as values, they
 * cannot be used to register handlers with properties that are only
 * known at runtime. It is therefore possible to specify a 
 * {@link Handler} annotation with element `dynamic=true`. Such a 
 * handler must be added explicitly by invoking 
 * {@link Evaluator#add(ComponentType, String, Object)} or
 * {@link Evaluator#add(ComponentType, String, Object, Object, int)},
 * thus specifying some of the handler's properties dynamically (i.e.
 * at runtime).
 * 
 * A special case is the usage of a channel that is only known at
 * runtime. If there are several handlers for events on such a
 * channel, a lot of methods will become dynamic. To avoid this,
 * {@link Component}s support a {@link ChannelReplacements}
 * parameter in their constructor. Using this, it is possible
 * to specify a specially defined {@link Channel} class in the
 * annotation that is replaced by a channel that is only known
 * at runtime.
 * 
 * If a method with a handler annotation is overwritten in a
 * derived class, the annotation is overwritten as well. The
 * annotated method of the base class is no longer invoked as
 * handler and the method of the derived class is only invoked
 * as handler if it defines its own handler annotation.
 * 
 * @see Component#channel()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@HandlerDefinition(evaluator = Handler.Evaluator.class)
public @interface Handler {

    /** The default value for the <code>events</code> parameter of
     * the annotation. Indicates that the parameter is not used. */
    final class NoEvent extends Event<Void> {
    }

    /** The default value for the <code>channels</code> parameter of
     * the annotation. Indicates that the parameter is not used. */
    final class NoChannel extends ClassChannel {
    }

    /**
     * Specifies classes of events that the handler is to receive.
     * 
     * ```java
     * class SampleComponent extends Component {
     * 
     *    {@literal @}Handler
     *     public void onStart(Start event) {
     *         // Invoked for Start events on the component's channel,
     *         // event object made available
     *     }
     * 
     *    {@literal @}Handler(events=Start.class)
     *     public void onStart() {
     *         // Invoked for Start events on the component's channel,
     *         // not interested in the event object
     *     }
     * 
     *    {@literal @}Handler(events={Start.class, Stop.class})
     *     public void onStart(Event<?> event) {
     *         // Invoked for Start and Stop events on the component's
     *         // channel, event made available (may need casting to 
     *         // access specific properties) 
     *     }
     * }
     * ```
     * 
     * @return the event classes
     */
    @SuppressWarnings("rawtypes")
    Class<? extends Event>[] events() default NoEvent.class;

    /**
     * Specifies names of {@link NamedEvent}s that the handler is to receive.
     * 
     * ```java
     * class SampleComponent extends Component {
     * 
     *    {@literal @}Handler(namedEvents="Test")
     *     public void onTest(Event<?> event) {
     *         // Invoked for (named) "Test" events (new NamedEvent("Test")) 
     *         // on the component's channel, event object made available
     *     }
     * }
     * ```
     * 
     * @return the event names
     */
    String[] namedEvents() default "";

    /**
     * Specifies classes of channels that the handler listens on. If none
     * are specified, the component's channel is used.
     * 
     * ```java
     * class SampleComponent extends Component {
     * 
     *    {@literal @}Handler(channels=Feedback.class)
     *     public void onStart(Start event) {
     *         // Invoked for Start events on the "Feedback" channel
     *         // (class Feedback implements Channel {...}),
     *         // event object made available
     *     }
     * }
     * ```
     * 
     * Specifying `channels=Channel.class` make the handler listen
     * for all events, independent of the channel that they are fired on.
     * 
     * Specifying `channels=Self.class` make the handler listen
     * for events that are fired on the conponent.
     * 
     * @return the channel classes
     */
    Class<? extends Channel>[] channels() default NoChannel.class;

    /**
     * Specifies names of {@link NamedChannel}s that the handler listens on.
     * 
     * ```java
     * class SampleComponent extends Component {
     * 
     *    {@literal @}Handler(namedChannels="Feedback")
     *     public void onStart(Start event) {
     *         // Invoked for Start events on the (named) channel "Feedback"
     *         // (new NamedChannel("Feedback")), event object made available
     *     }
     * }
     * ```
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
     * Returns {@code true} if the annotated method defines a
     * dynamic handler. A dynamic handler must be added to the set of
     * handlers of a component explicitly at run time using
     * {@link Evaluator#add(ComponentType, String, Object)}
     * or {@link Evaluator#add(ComponentType, String, Object, Object, int)}.
     * 
     * ```java
     * class SampleComponent extends Component {
     * 
     *     SampleComponent() {
     *         Handler.Evaluator.add(this, "onStartDynamic", someChannel);
     *     }
     * 
     *    {@literal @}Handler(dynamic=true)
     *     public void onStartDynamic(Start event) {
     *         // Only invoked if added as handler at runtime
     *     }
     * }
     * ```
     * 
     * @return the result
     */
    boolean dynamic() default false;

    /**
     * Excludes the handler from channel matching against its component's
     * default criterion. The typical use case for this annotation is
     * a converter component that receives events from some source channel
     * and then fires the same kind of events with modified data using 
     * itself as the (source) channel. In this case, the events
     * generated by the component must not be processed by the component
     * although they are fired using the component as channel. So while it
     * is useful to be able to target a specific component (using it as 
     * channel) in general, it isn't in this special case and can therefore
     * be turned off with this annotation.
     * 
     * Of course, it would also be possible to work around the ambiguity
     * by firing the conversion results on an extra channel. But it is 
     * quite intuitive to use the component itself as (source) channel.
     *
     * @return true, if set
     */
    boolean excludeSelf() default false;

    /**
     * This class provides the {@link Evaluator} for the 
     * {@link Handler} annotation provided by the core package. It 
     * implements the behavior as described for the annotation. 
     */
    class Evaluator implements HandlerDefinition.Evaluator {

        @Override
        public HandlerScope scope(
                ComponentType component, Method method,
                ChannelReplacements channelReplacements) {
            Handler annotation = method.getAnnotation(Handler.class);
            if (annotation == null || annotation.dynamic()) {
                return null;
            }
            return new Scope(component, method, annotation,
                channelReplacements, null, null);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jgrapes.core.annotation.HandlerDefinition.Evaluator#getPriority()
         */
        @Override
        public int priority(Annotation annotation) {
            return ((Handler) annotation).priority();
        }

        /**
         * Adds the given method of the given component as a dynamic handler for
         * a specific event and channel. The method with the given name must be
         * annotated as dynamic handler and must have a single parameter of type
         * {@link Event} (or a derived type as appropriate for the event type to
         * be handled). It can have an optional parameter of type 
         * {@link Channel}.
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
         *            the channel value that should be used for matching 
         *            an event's channel with this handler. This is equivalent 
         *            to a `channels`/`namedChannels` parameter with a single
         *            value in the handler annotation, but
         *            here all kinds of Objects are allowed as values. As a
         *            convenience, if the actual object provided is a
         *            {@link Channel}, its default criterion is used for 
         *            matching.
         * @param priority
         *            the priority of the handler
         */
        public static void add(ComponentType component, String method,
                Object eventValue, Object channelValue, int priority) {
            addInternal(component, method, eventValue, channelValue, priority);
        }

        /**
         * Add a handler like 
         * {@link #add(ComponentType, String, Object, Object, int)}
         * but take the values for event and priority from the annotation.
         * 
         * @param component the component
         * @param method the name of the method that implements the handler
         * @param channelValue the channel value that should be used 
         * for matching an event's channel with this handler
         */
        public static void add(ComponentType component, String method,
                Object channelValue) {
            addInternal(component, method, null, channelValue, null);
        }

        @SuppressWarnings({ "PMD.CyclomaticComplexity",
            "PMD.AvoidBranchingStatementAsLastInLoop",
            "PMD.CognitiveComplexity" })
        private static void addInternal(ComponentType component, String method,
                Object eventValue, Object channelValue, Integer priority) {
            try {
                if (channelValue instanceof Channel) {
                    channelValue = ((Eligible) channelValue).defaultCriterion();
                }
                for (Method m : component.getClass().getMethods()) {
                    if (!m.getName().equals(method)) {
                        continue;
                    }
                    for (Annotation annotation : m.getDeclaredAnnotations()) {
                        Class<?> annoType = annotation.annotationType();
                        if (!annoType.equals(Handler.class)) {
                            continue;
                        }
                        HandlerDefinition hda
                            = annoType.getAnnotation(HandlerDefinition.class);
                        if (hda == null
                            || !Handler.class.isAssignableFrom(annoType)
                            || !((Handler) annotation).dynamic()) {
                            continue;
                        }
                        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                        Scope scope = new Scope(component, m,
                            (Handler) annotation, Collections.emptyMap(),
                            eventValue == null ? null
                                : new Object[] { eventValue },
                            new Object[] { channelValue });
                        Components.manager(component).addHandler(m, scope,
                            priority == null
                                ? ((Handler) annotation).priority()
                                : priority);
                        return;
                    }
                }
                throw new IllegalArgumentException(
                    "No method named \"" + method + "\" with DynamicHandler"
                        + " annotation and correct parameter list.");
            } catch (SecurityException e) {
                throw (RuntimeException) new IllegalArgumentException()
                    .initCause(e);
            }
        }

        /**
         * The handler scope implementation used by the evaluator.
         */
        private static class Scope implements HandlerScope {

            private final Set<Object> eventCriteria = new HashSet<>();
            private final Set<Object> channelCriteria = new HashSet<>();

            /**
             * Instantiates a new scope.
             *
             * @param component the component
             * @param method the method
             * @param annotation the annotation
             * @param channelReplacements the channel replacements
             * @param eventValues the event values
             * @param channelValues the channel values
             */
            @SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.NcssCount",
                "PMD.NPathComplexity", "PMD.UseVarargs",
                "PMD.AvoidDeeplyNestedIfStmts", "PMD.CollapsibleIfStatements",
                "PMD.CognitiveComplexity" })
            public Scope(ComponentType component, Method method,
                    Handler annotation,
                    Map<Class<? extends Channel>, Object[]> channelReplacements,
                    Object[] eventValues, Object[] channelValues) {
                if (!HandlerDefinition.Evaluator.checkMethodSignature(method)) {
                    throw new IllegalArgumentException("Method \""
                        + method.toString() + "\" cannot be used as"
                        + " handler (wrong signature).");
                }
                if (eventValues != null) { // NOPMD, != is easier to read
                    eventCriteria.addAll(Arrays.asList(eventValues));
                } else {
                    // Get all event values from the handler annotation.
                    if (annotation.events()[0] != Handler.NoEvent.class) {
                        eventCriteria
                            .addAll(Arrays.asList(annotation.events()));
                    }
                    // Get all named events from the annotation and add to event
                    // keys.
                    if (!annotation.namedEvents()[0].isEmpty()) {
                        eventCriteria.addAll(
                            Arrays.asList(annotation.namedEvents()));
                    }
                    // If no event types are given, try first parameter.
                    if (eventCriteria.isEmpty()) {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length > 0) {
                            if (Event.class.isAssignableFrom(paramTypes[0])) {
                                eventCriteria.add(paramTypes[0]);
                            }
                        }
                    }
                }

                if (channelValues != null) { // NOPMD, != is easier to read
                    channelCriteria.addAll(Arrays.asList(channelValues));
                } else {
                    // Get channel values from the annotation.
                    boolean addDefaultChannel = false;
                    if (annotation.channels()[0] != Handler.NoChannel.class) {
                        for (Class<?> c : annotation.channels()) {
                            if (c == Self.class) {
                                if (!(component instanceof Channel)) {
                                    throw new IllegalArgumentException(
                                        "Canot use channel This.class in "
                                            + "annotation of " + method
                                            + " because " + getClass().getName()
                                            + " does not implement Channel.");
                                }
                                // Will be added anyway, see below, but
                                // channelCriteria must not remain empty,
                                // else the default channel is added.
                                channelCriteria.add(
                                    ((Channel) component).defaultCriterion());
                            } else if (c == Channel.Default.class) {
                                addDefaultChannel = true;
                            } else {
                                if (channelReplacements != null
                                    && channelReplacements.containsKey(c)) {
                                    channelCriteria.addAll(
                                        Arrays.asList(channelReplacements
                                            .get(c)));
                                } else {
                                    channelCriteria.add(c);
                                }
                            }
                        }
                    }
                    // Get named channels from annotation and add to channel
                    // keys.
                    if (!annotation.namedChannels()[0].isEmpty()) {
                        channelCriteria.addAll(
                            Arrays.asList(annotation.namedChannels()));
                    }
                    if (channelCriteria.isEmpty() || addDefaultChannel) {
                        channelCriteria.add(Components.manager(component)
                            .channel().defaultCriterion());
                    }
                }
                // Finally, a component always handles events
                // directed at it directly.
                if (component instanceof Channel && !annotation.excludeSelf()) {
                    channelCriteria.add(
                        ((Channel) component).defaultCriterion());
                }

            }

            @Override
            @SuppressWarnings("PMD.CognitiveComplexity")
            public boolean includes(Eligible event, Eligible[] channels) {
                for (Object eventValue : eventCriteria) {
                    if (event.isEligibleFor(eventValue)) {
                        // Found match regarding event, now try channels
                        for (Eligible channel : channels) {
                            for (Object channelValue : channelCriteria) {
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

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder(100);
                builder.append("Scope [");
                if (eventCriteria != null) {
                    builder.append("handledEvents=")
                        .append(eventCriteria.stream().map(crit -> {
                            if (crit instanceof Class) {
                                return Components.className((Class<?>) crit);
                            }
                            return crit.toString();
                        }).collect(Collectors.toSet())).append(", ");
                }
                if (channelCriteria != null) {
                    builder.append("handledChannels=").append(channelCriteria);
                }
                builder.append(']');
                return builder.toString();
            }

        }
    }
}
