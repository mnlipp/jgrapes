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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdrupes.internal.ComponentNode;
import org.jdrupes.internal.WeakIdentityHashMap;

/**
 * @author mnl
 *
 */
public class Manager {

	private Map<Component, ComponentNode> nodes 
		= new WeakIdentityHashMap<Component, ComponentNode>();
	private List<ComponentNode> trees = new ArrayList<ComponentNode>();

	/**
	 * Attach tree to manager. Updates component to node mapping and
	 * sets components' manager.
	 * 
	 * @param node the root of the tree to be attached
	 */
	private void attachTree (ComponentNode node) {
		nodes.put(node.getComponent(), node);
		node.setManagerForNode(this);
		for (ComponentNode child: node.getChildren()) {
			attachTree(child);
		}
	}
	
	/**
	 * Detach tree from manager. Updates component to node mapping and
	 * clears components' manager.
	 * 
	 * @param node the root of the tree to be detached
	 */
	private void detachTree (ComponentNode node) {
		nodes.remove(node.getComponent());
		node.setManagerForNode(null);
		for (ComponentNode child: node.getChildren()) {
			detachTree(child);
		}
	}

	/**
	 * Get a node for the component. If the component is attached to
	 * the manager, this is the existing node, else a new node is created.
	 * 
	 * @param component the component
	 * @return the node
	 */
	private ComponentNode getNode(Component component) {
		if (component == null) {
			return null;
		}
		ComponentNode node = nodes.get(component);
		if (node == null) {
			node = new ComponentNode (null, component);
		}
		return node;
	}

	/**
	 * Attach the given component to the manager as root component.
	 * 
	 * @param component the component to attach
	 * @return the component
	 */
	public Component attach (Component component) {
		attach((ComponentRef)null, component);
		return component;
	}
	
	/**
	 * Attach the referenced component to the manager as root component.
	 * 
	 * @param component the component to attach
	 * @return the component
	 */
	public Component attach (ComponentRef component) {
		attach((ComponentRef)null, component);
		return component.getComponent();
	}

	/**
	 * Attach the given child component to the given parent component
	 * that must already be attached to the manager. 
	 * 
	 * @param parent the parent component
	 * @param child the child component
	 * @return the parent component
	 */
	public Component attach (Component parent, Component child) {
		ComponentNode parentNode = nodes.get(parent);
		if (parentNode == null) {
			throw new IllegalArgumentException("Parent is not managed");
		}
		return attach(parentNode, getNode(child)).getComponent();
	}	

	/**
	 * Attach the referenced child component to the given parent component
	 * that must already be attached to the manager. 
	 * 
	 * @param parent the parent component
	 * @param child the child component
	 * @return the parent component
	 */
	public ComponentRef attach (Component parent, ComponentRef child) {
		return attach(getNode(parent), child);
	}
	
	/**
	 * Attach the given child component to the referenced parent component.
	 * Return the reference to the parent (the detached tree).
	 * 
	 * @param parent the parent component
	 * @param child the child component
	 * @return the parent component
	 */
	public ComponentRef attach (ComponentRef parent, Component child) {
		return attach(parent, getNode(child));
	}	

	/**
	 * Attach the referenced child component to the referenced parent 
	 * component. Return the reference to the parent (the detached tree).
	 * 
	 * @param parent the parent component
	 * @param child the child component
	 * @return the parent component
	 */
	public ComponentRef attach (ComponentRef parent, ComponentRef child) {
		if (child == null) {
			throw new IllegalArgumentException("Cannot attach null child");
		}
		ComponentNode parentNode = (ComponentNode)parent;
		ComponentNode childNode = (ComponentNode)child;
		
		// Make sure that child is not in tree
		if (childNode.getManager() != null) {
			// Child has parent, detach from it
			childNode = (ComponentNode)detach(child.getComponent());
		}
		
		// If parent is null, we're almost done
		if (parentNode == null) {
			attachTree (childNode);
			trees.add(childNode);
			return null; 
		}

		// Else attach to parent and maybe update index
		parentNode.addChild(childNode);
		if (parentNode.getManager() != null) {
			attachTree(childNode);
		}

		// Return parent node
		return parentNode;
	}
	
	/**
	 * Detach the tree that has component as root from the manager.
	 * Note that the given component need not be attached to the manager,
	 * therefore the method may also be used to get a reference to a
	 * (detached) component.
	 * 
	 * @param component the component tree to detach
	 * @return a reference to the component tree
	 */
	public ComponentRef detach(Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			return new ComponentNode (null, component);
		}
		if (node.getParent() == null) {
			trees.remove(node);
		} else {
			node.getParent().removeChild(node);
		}
		detachTree(node);
		return node;
	}
		
	/**
	 * Get all component trees (root components) managed by this
	 * manager.
	 * 
	 * @return the root components
	 */
	public Set<Component> getTrees() {
		Set<Component> res = new HashSet<Component>();
		for (ComponentNode node: trees) {
			res.add (node.getComponent());
		}
		return Collections.unmodifiableSet(res);
	}

	/**
	 * Get the paremt component of the given component.
	 * 
	 * @param component the child component
	 * @return the parent or <code>null</code> if the component is
	 * the root of a component tree.
	 */
	public Component getParent(Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			throw new IllegalArgumentException("Component is not managed");
		}
		ComponentNode parentNode = node.getParent();
		if (parentNode == null) {
			return null;
		}
		return parentNode.getComponent();
	}
	
	/**
	 * Get all children of the given component.
	 * 
	 * @param component the component
	 * @return the list of children
	 */
	public List<Component> getChildren(Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			throw new IllegalArgumentException("Component is not managed");
		}
		List<Component> res = new ArrayList<Component>();
		for (ComponentNode child: node.getChildren()) {
			res.add(child.getComponent());
		}
		return res;
	}
	
	/**
	 * Print the component tree that has component as root 
	 * to the given print stream.
	 * 
	 * @param out the print stream
	 * @param component the root of the component tree
	 */
	public void printComponentTree (PrintStream out, Component component) {
		ComponentNode node = nodes.get(component);
		if (node != null) {
			printComponentTree(out, node, 0);
		}
	}
	
	private void printComponentTree 
		(PrintStream out, ComponentNode node, int indent) {
		for (int i = 0; i < indent; i++) {
			out.print(" ");
		}
		out.print(node.getComponent().toString());
		out.println();
		for (ComponentNode child: node.getChildren()) {
			printComponentTree(out, child, indent + 1);
		}
	}
	
	@Override
	public String toString() {
		OutputStream os = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(os);
		if (trees.size() > 0) {
			for (ComponentNode node: trees) {
				printComponentTree(out, node, 0);
			}
		}
		out.flush();
		return os.toString();
	}
}
