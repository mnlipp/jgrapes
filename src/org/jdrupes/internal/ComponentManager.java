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
package org.jdrupes.internal;

import java.util.List;

import org.jdrupes.Channel;
import org.jdrupes.Component;
import org.jdrupes.Event;
import org.jdrupes.Manager;

/**
 * The ComponentManager interface defines the methods for managing
 * a component and its handlers within the component tree. 
 * 
 * @author mnl
 */
public interface ComponentManager extends Iterable<Component> {

	/**
	 * Detached the component managed by this manager (and its children,
	 * if any) from the component tree that it currently belongs to.
	 * 
	 * @return the component, for comfortable chaining
	 */
	Component detach ();

	/**
	 * Adds the given component node as a child.
	 * 
	 * @param child the component to add
	 * @return the component's manager, for comfortable chaining
	 */
	Manager addChild (Component child);
	
	/**
	 * Remove the given component from the set of children.
	 * 
	 *  @param child the component to be removed
	 */
	void removeChild(Component child);
	
	/**
	 * Return the child components of this component as unmodifiable list.
	 * 
	 * @return the child components
	 */
	List<Component> getChildren();

	/**
	 * Return the component's parent.
	 * 
	 * @return the parent component or <code>null</code> if the
	 * component is not registered with another component
	 */
	Component getParent();
	
	/**
	 * Return the root of the tree the component belongs to.
	 * 
	 * @return the root
	 */
	Component getRoot();
	
	/**
	 * Add a handler for a specific event and channel. The method
	 * with the given name must have a single argument of type
	 * {@link Event} (or a derived type as appropriate for the
	 * events to be handled).
	 * 
	 * @param eventKey the event key that should be used for matching
	 * this handler with an event
	 * @param channelKey the channel key that should be used for matching
	 * this handler with an event
	 * @param method the method that implements the handler
	 */
	void addHandler(Object eventKey, Object channelKey, String method);
	
	/**
	 * Fire the given event on the component's channel.
	 * 
	 * @param event the event to fire
	 */
	void fire(Event event);
	
}
