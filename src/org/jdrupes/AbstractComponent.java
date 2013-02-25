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
 * This is the base class for a new component. Components can be
 * created by deriving from this class or by implementing 
 * the interface {@link Component}.
 */
public class AbstractComponent extends ComponentNode 
	implements Component, ChannelMatchable {

	/**
	 * 
	 */
	public AbstractComponent() {
		super();
		initComponentsHandlers();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.ComponentBase#getComponent()
	 */
	@Override
	public Component getComponent() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Component#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return Channel.BROADCAST_CHANNEL;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#getMatchKey()
	 */
	@Override
	public Object getMatchKey() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.Matchable#matches(java.lang.Object)
	 */
	@Override
	public boolean matches(Object handlerKey) {
		return handlerKey.equals(Channel.class) || handlerKey == this;
	}

}
