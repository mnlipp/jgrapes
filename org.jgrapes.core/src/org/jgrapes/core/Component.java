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
package org.jgrapes.core;

import org.jgrapes.core.internal.ComponentVertex;

/**
 * A base class for components. In general, new component types can 
 * be created by deriving from this class or by implementing 
 * the interface {@link ComponentType}. 
 * <P>
 * This class implements the {@link Manager} interface. In contrary 
 * to component types that implement {@link ComponentType}, derived 
 * classes therefore don't need a manager attribute to get access to the 
 * component management methods provided by this interface.
 * <P>
 * The class also implements the {@code Channel} interface.
 * This allows instances to be used as targets for events. 
 * 
 * @author Michael N. Lipp
 * @see ComponentType
 */
public abstract class Component extends ComponentVertex 
	implements ComponentType, Channel {

	private Channel componentChannel;
	
	/**
	 * Creates a new component base with its channel set to
	 * itself.
	 */
	public Component() {
		super();
		componentChannel = this;
		initComponentsHandlers();
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
		initComponentsHandlers();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.ComponentVertex#getComponent()
	 */
	@Override
	protected ComponentType getComponent() {
		return this;
	}

	/**
	 * Returns the channel associated with the component.
	 * 
	 * @return the channel as assigned by the constructor.
	 * 
	 * @see org.jgrapes.core.Manager#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return componentChannel;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.Matchable#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return handlerKey.equals(Channel.class) || handlerKey == getMatchKey();
	}

}
