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

import org.jdrupes.events.Start;
import org.jdrupes.events.Started;
import org.jdrupes.internal.ComponentNode;
import org.jdrupes.internal.ComponentProxy;

/**
 * @author mnl
 *
 */
public class Utils {

	private Utils() {
	}
	
	/**
	 * Return a component's manager. Components that inherit
	 * from {@link org.jdrupes.AbstractComponent} will simply return
	 * the component as they are their own managers.
	 * 
	 * Components that implement {@link Component} but don't inherit from 
	 * {@link org.jdrupes.AbstractComponent} return the value of the
	 * attribute marked as manager slot. A value is automatically assigned
	 * to this attribute when a component is attached to the component tree.
	 * When this method is invoked for such a component that hasn't been 
	 * attached to a component tree yet, it makes the component the root
	 * of a new tree and returns its manager.
	 * 
	 * @param component the component
	 * @return the component with its manager attribute set
	 */
	public static Manager manager (Component component) {
		Manager manager = ComponentNode.getComponentNode(component);
		if (manager != null) {
			return manager;
		}
		return new ComponentProxy(component);
	}

	/**
	 * Fires a {@link Start} event with an associated
	 * {@link Started} completion event on the broadcast channel
	 * of the given application. 
	 * 
	 * @param application the application to start
	 */
	public static void start(Component application) {
		manager(application).fire
			((new Start()).addCompletedEvent(Started.class),
			 Channel.BROADCAST);
	}
	
}
