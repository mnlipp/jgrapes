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

import org.jdrupes.internal.ComponentManager;

/**
 * @author mnl
 *
 */
public class Utils {

	private Utils() {
	}
	
	/**
	 * Make sure that the given component has a manager. Newly created
	 * components that implement {@link Component} haven't their manager
	 * attribute set yet. The manager attribute is automatically set 
	 * when the component is attached to another component, but the root 
	 * component of a tree must have its manager attribute set explicitly 
	 * using this method.
	 * 
	 * If the method is invoked for a component that already has
	 * a manager, it simply returns the value of the manager attribute.
	 * If it is called for a component that extends {@link AbstractComponent},
	 * it returns the component itself.
	 * 
	 * @param component the component
	 * @return the component with its manager attribute set
	 */
	public static Manager ensureManager (Component component) {
		Manager manager = ComponentManager.getComponentBase(component);
		if (manager != null) {
			return manager;
		}
		return new ComponentManager(component);
	}
	
}
