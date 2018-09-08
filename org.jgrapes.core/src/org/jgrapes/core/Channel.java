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

package org.jgrapes.core;

import org.jgrapes.core.annotation.Handler;

/**
 * Instances of this this interface can be used as a communication 
 * bus for sending events between components. The instances work
 * as identifiers of channels. Their only functionality is defined
 * by the {@link Eligible} interface, which allows a channel
 * (used as attribute of an {@link Event}) to be matched against 
 * a criterion specified in a {@link Handler}.
 * 
 * The need to use the {@link Eligible} interface for comparison
 * arises from the fact that we cannot use objects as values in
 * annotations. It must therefore be possible to match channels
 * (objects) against criteria that can be expressed as constant 
 * values.
 * 
 * Some values have been defined to represent special criteria.
 * 
 * * If the value `Channel.class` is specified as criterion in
 *   a handler, all channel instances match. It is the "catch-all"
 *   criterion.
 * 
 * * If the value `{@link Default}.class` is specified as criterion
 *   in a handler, the channels from an {@link Event} are
 *   matched agains the criterion from the component's channel
 *   (returned by the {@link Manager#channel() channel()} method).  
 * 
 * The predefined {@link #BROADCAST} channel is a channel instance
 * that implements the {@link Eligible} interface in such a way that
 * all criteria match. Events fired on the {@link #BROADCAST} channel
 * will therefore be accepted by all handlers (as its name suggests).
 * 
 * For ordinary usage, the implementing classes {@link ClassChannel}
 * and {@link NamedChannel} should be sufficient. If another type of
 * `Channel` is needed, its implementation must make sure that 
 * {@link Eligible#isEligibleFor(Object)} returns
 * `true` if called with `Channel.class` as parameter, else channels 
 * of the new type will not be delivered to "catch-all" handlers.
 * 
 * Objects of type <code>Channel</code> must be immutable.
 * 
 * @see Channel#BROADCAST
 */
public interface Channel extends Eligible {

    /**
     * A special channel object that can be passed as argument to 
     * the constructor of {@link Component#Component(Channel)}. 
     * Doing this sets the component's channel to the component 
     * (which is not available as argument when calling the 
     * constructor).
     * 
     * @see Component#Component(Channel)
     */
    Channel SELF = new ClassChannel();

    /**
     * This interface's class can be used to specify the component's 
     * channel (see {@link Component#channel()}) as criterion in 
     * handler annotations.
     * 
     * Using the component's channel for comparison is the default 
     * if no channels are specified in the annotation, so specifying 
     * only this class in the handler annotation is equivalent
     * to specifying no channel at all. This special channel type is required
     * if you want to specify a handler that handles events fired on the 
     * component's channel or on additional channels.
     */
    interface Default extends Channel {
    }

    /**
     * A special channel instance that can be used to send events to
     * all components.
     */
    Channel BROADCAST = new ClassChannel() {

        /**
         * Returns <code>Channel.class</code>, the value that must
         * by definition be matched by any channel.
         * 
         * @return <code>Channel.class</code>
         */
        @Override
        public Object defaultCriterion() {
            return Channel.class;
        }

        /**
         * Always returns {@code true} because the broadcast channel
         * is matched by every channel.
         * 
         * @return {@code true}
         */
        @Override
        public boolean isEligibleFor(Object criterion) {
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.core.ClassChannel#toString()
         */
        @Override
        public String toString() {
            return "BROADCAST";
        }
    };

    /**
     * Returns a textual representation of a channel's criterion.
     * 
     * @param criterion the criterion
     * @return the representation
     */
    static String criterionToString(Object criterion) {
        StringBuilder builder = new StringBuilder();
        if (criterion instanceof Class) {
            if (criterion == Channel.class) {
                builder.append("BROADCAST");
            } else {
                builder.append(Components.className((Class<?>) criterion));
            }
        } else {
            builder.append(criterion);
        }
        return builder.toString();
    }

    /**
     * Returns a textual representation of a channel.
     * 
     * @param channel the channel
     * @return the representation
     */
    static String toString(Channel channel) {
        if (channel == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        if (channel instanceof ClassChannel
            || channel instanceof NamedChannel) {
            builder.append(criterionToString(channel.defaultCriterion()));
        } else if (channel == channel.defaultCriterion()) {
            builder.append(Components.objectName(channel));
        } else {
            builder.append(channel.toString());
        }
        return builder.toString();
    }

    /**
     * Returns a textual representation of an array of channels.
     * 
     * @param channels the channels
     * @return the representation
     */
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.UseVarargs" })
    static String toString(Channel[] channels) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean first = true;
        for (Channel c : channels) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(Channel.toString(c));
            first = false;
        }
        builder.append(']');
        return builder.toString();
    }

}
