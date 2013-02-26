/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.jdrupes;

import org.jdrupes.internal.ComponentNode;

/**
 * A convenience base class for new components. In general, components can 
 * be created by deriving from this class or by implementing 
 * the interface {@link Component}. 
 * <P>
 * By deriving from this class,
 * a component implementation gets direct access to the methods of the
 * {@link Manager} interface and doesn't have to access the
 * manager using a manager attribute.
 * 
 * @see Component
 */
public class AbstractComponent extends ComponentNode 
	implements Component, Channel {

	// Don't use "this" to prevent overridden "equals" in derived classes
	// from causing trouble
	private Object matchKey = new Object();
	
	private Channel componentChannel = BROADCAST;
	
	/**
	 * Create the new component base with its channel set to
	 * the broadcast channel.
	 */
	public AbstractComponent() {
		super();
		initComponentsHandlers();
	}

	/**
	 * Create the new component base with its channel set to
	 * the given channel.
	 * 
	 * @param componentChannel the channel that the component's 
	 * handlers listen on by default and that 
	 * {@link Manager#fire(Event)} sends the event to 
	 */
	public AbstractComponent(Channel componentChannel) {
		super();
		this.componentChannel = componentChannel;
		initComponentsHandlers();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.ComponentNode#getComponent()
	 */
	@Override
	protected Component getComponent() {
		return this;
	}

	/**
	 * Returns the channel associated with the component.
	 * 
	 * @return the channel passed to the constructor
	 * or <code>BROADCAST_CHANNEL</code>
	 * 
	 * @see org.jdrupes.Manager#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return componentChannel;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return matchKey;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return handlerKey.equals(Channel.class) || handlerKey == matchKey;
	}

}
