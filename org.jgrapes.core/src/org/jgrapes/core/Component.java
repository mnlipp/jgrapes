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

import java.util.logging.Logger;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.internal.ComponentVertex;

/**
 * This class can be used as base class for implementing a component. 
 * <P>
 * This class implements the {@link Manager} interface. Contrary 
 * to classes that only implement {@link ComponentType}, derived 
 * classes therefore don't need a manager attribute to get access to the 
 * component management methods provided by this interface.
 * <P>
 * This class also implements the {@code Channel} interface in such a way
 * that each instance of this class can be used as an independent
 * channel. Note that events that have a component as one of their
 * channels are always handled by the component's handlers, i.e. in 
 * addition to the channels explicitly defined for a handler. 
 * 
 * @see Handler
 * @see ComponentType
 */
public abstract class Component extends ComponentVertex
        implements ComponentType, Channel {

    @SuppressWarnings("PMD.FieldNamingConventions")
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final Channel componentChannel;

    /**
     * Creates a new component base with its channel set to
     * itself.
     */
    public Component() {
        super();
        componentChannel = this;
        initComponentsHandlers(null);
    }

    /**
     * Creates a new component base with its channel set to the given 
     * channel. As a special case {@link Channel#SELF} can be
     * passed to the constructor to make the component use itself
     * as channel. The special value is necessary as you 
     * obviously cannot pass an object to be constructed to its 
     * constructor.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     */
    public Component(Channel componentChannel) {
        super();
        if (componentChannel == SELF) {
            this.componentChannel = this;
        } else {
            this.componentChannel = componentChannel;
        }
        initComponentsHandlers(null);
    }

    /**
     * Creates a new component base like {@link #Component(Channel)}
     * but with channel mappings for {@link Handler} annotations.
     *
     * @param componentChannel the channel that the component's
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to
     * @param channelReplacements the channel replacements to apply
     * to the `channels` elements of the {@link Handler} annotations
     */
    public Component(
            Channel componentChannel, ChannelReplacements channelReplacements) {
        super();
        if (componentChannel == SELF) {
            this.componentChannel = this;
        } else {
            this.componentChannel = componentChannel;
        }
        initComponentsHandlers(channelReplacements);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.internal.ComponentVertex#setName(java.lang.String)
     */
    @Override
    public Component setName(String name) {
        super.setName(name);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.internal.ComponentVertex#getComponent()
     */
    @Override
    public Component component() {
        return this;
    }

    /**
     * Returns the channel associated with the component.
     * 
     * @return the channel as assigned by the constructor.
     * 
     * @see org.jgrapes.core.Manager#channel()
     */
    @Override
    public Channel channel() {
        return componentChannel;
    }

    /**
     * Return the object itself as value.
     */
    @Override
    public Object defaultCriterion() {
        return this;
    }

    /**
     * Matches the object itself (using identity comparison) or the
     * {@link Channel} class.
     * 
     * @see Channel#isEligibleFor(Object)
     */
    @Override
    public boolean isEligibleFor(Object value) {
        return value.equals(Channel.class)
            || value == defaultCriterion();
    }

}
