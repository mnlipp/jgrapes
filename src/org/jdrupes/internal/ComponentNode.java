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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdrupes.Component;
import org.jdrupes.Manager;

/**
 * @author mnl
 *
 */
public class ComponentNode {

	private Component component;
	private ComponentNode parent = null;
	private List<ComponentNode> children = new ArrayList<ComponentNode>();
	
	public ComponentNode (Manager manager, Component component) {
		this.component = component;
		setManager(manager);
	}

	public void setManager (Manager manager) {
		try {
			Field field = component.getClass().getDeclaredField("manager");
			if (!field.isAccessible()) {
				field.setAccessible(true);
				field.set(component, manager);
				field.setAccessible(false);
			} else {
				field.set(component, manager);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		for (ComponentNode node: children) {
			node.setManager(manager);
		}
	}
	
	/**
	 * Return the component attached to this node.
	 * 
	 * @return the component;
	 */
	public Component getComponent() {
		return component;
	}
	
	/**
	 * Adds the given component node as a child.
	 * 
	 * @param child the component to add
	 * @return the component added
	 */
	public ComponentNode addChild (ComponentNode child) {
		children.add(child);
		child.parent = this;
		return child;
	}
	
	/**
	 * Remove the given component from the set of children.
	 * 
	 *  @param child the component to be removed
	 */
	public void removeChild(ComponentNode child) {
		children.remove(child);
		child.parent = null;
	}
	
	/**
	 * Return the component's parent.
	 * 
	 * @return the parent component or <code>null</code> if the
	 * component is not registered with another component
	 */
	public ComponentNode getParent() {
		return parent;
	}
	
	/**
	 * Return the child components of this component as unmodifiable list.
	 * 
	 * @return the child components
	 */
	public List<ComponentNode> getChildren() {
		return Collections.unmodifiableList(children);
	}

}
