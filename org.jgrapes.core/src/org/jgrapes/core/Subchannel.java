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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a subchannel. Subchannels delegate the invocations of a
 * {@link Channel}'s methods to their respective main channel. Events fired on
 * a subchannel are therefore handled by the framework as if they were fired on
 * the main channel. Firing events on a subchannel instance instead of on the
 * main channel is a means to associate several events with a common context.
 */
public interface Subchannel extends Channel, Associator {

    /**
     * Returns the main channel.
     * 
     * @return the mainChannel
     */
    Channel mainChannel();

    /**
     * Returns the main channel's match value.
     * 
     * @see Channel#defaultCriterion()
     */
    @Override
    default Object defaultCriterion() {
        return mainChannel().defaultCriterion();
    }

    /**
     * Delegates to main channel.
     * 
     * @see Channel#isEligibleFor(Object)
     */
    @Override
    default boolean isEligibleFor(Object value) {
        return mainChannel().isEligibleFor(value);
    }

    /**
     * Returns a string representation of the channel.
     *
     * @param subchannel the subchannel
     * @return the string
     */
    static String toString(Subchannel subchannel) {
        StringBuilder builder = new StringBuilder();
        builder.append(Channel.toString(subchannel.mainChannel()))
            .append('{')
            .append(Components.objectName(subchannel))
            .append('}');
        return builder.toString();
    }

    /**
     * Creates a new subchannel of the given component's channel.
     *
     * @param component the component used to get the main channel
     * @return the subchannel
     */
    static Subchannel create(Component component) {
        return new DefaultSubchannel(component.channel());
    }

    /**
     * A simple implementation of {@link Subchannel}.
     */
    class DefaultSubchannel implements Subchannel {
        private final Channel mainChannel;
        private Map<Object, Object> contextData;

        /**
         * Creates a new instance with the given main channel and response
         * pipeline.  
         * 
         * @param mainChannel the main channel
         */
        public DefaultSubchannel(Channel mainChannel) {
            this.mainChannel = mainChannel;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.core.Subchannel#getMainChannel()
         */
        @Override
        public Channel mainChannel() {
            return mainChannel;
        }

        /**
         * Establishes a "named" association to an associated object. Note that 
         * anything that represents an id can be used as value for 
         * parameter `name`, it does not necessarily have to be a string.
         * 
         * @param by the "name"
         * @param with the object to be associated
         */
        @SuppressWarnings({ "PMD.ShortVariable", "unchecked" })
        public DefaultSubchannel setAssociated(Object by, Object with) {
            if (contextData == null) {
                contextData = new ConcurrentHashMap<>();
            }
            if (with == null) {
                contextData.remove(by);
            } else {
                contextData.put(by, with);
            }
            return this;
        }

        /**
         * Retrieves the associated object following the association 
         * with the given "name". This general version of the method
         * supports the retrieval of values of arbitrary types
         * associated by any "name" types. 
         * 
         * @param by the "name"
         * @param type the tape of the value to be retrieved
         * @param <V> the type of the value to be retrieved
         * @return the associate, if any
         */
        @SuppressWarnings("PMD.ShortVariable")
        public <V> Optional<V> associated(Object by, Class<V> type) {
            if (contextData == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(contextData.get(by))
                .filter(found -> type.isAssignableFrom(found.getClass()))
                .map(type::cast);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return Subchannel.toString(this);
        }

    }
}
