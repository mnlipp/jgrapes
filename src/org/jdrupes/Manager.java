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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdrupes.internal.ComponentNode;

/**
 * @author mnl
 *
 */
public class Manager {

	Map<Component, ComponentNode> nodes 
		= new IdentityHashMap<Component, ComponentNode>();
	Map<ComponentNode, Object> orphaned	
		= new WeakHashMap<ComponentNode, Object>();
	ComponentNode rootNode = null;

	private ComponentNode findOrphaned(Component component) {
		for (ComponentNode orphan: orphaned.keySet()) {
			if (orphan.getComponent() == component) {
				return orphan;
			}
		}
		return null;
	}
	
	public Component attach (Component parent, Component component) {
		ComponentNode parentNode = null;
		boolean connected = true;
		if (parent == null) {
			if (rootNode != null) {
				throw new IllegalStateException("Cannot have two roots");
			}
		} else {
			parentNode = nodes.get(parent);
			if (parentNode == null) {
				parentNode = findOrphaned(parent);
				if (parentNode != null) {
					connected = false;
				}
			}
			if (parentNode == null) {
				parentNode = new ComponentNode(null, parent);
				orphaned.put(parentNode, null);
			}
		}
		ComponentNode compNode = nodes.get(component);
		if (compNode != null) {
			detach(component);
		} else {
			compNode = findOrphaned(component);
			if (compNode != null) {
				orphaned.remove(compNode);
			} else {
				compNode = new ComponentNode(this, component);
			}
		}
		if (parentNode == null) {
			rootNode = compNode;
		} else {
			parentNode.addChild(compNode);
		}
		if (connected) {
			compNode.setManager(this);
			nodes.put(component, compNode);
		}
		return component;
	}
	
	public void detach(Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			return;
		}
		if (node.getParent() != null) {
			node.getParent().removeChild(node);
		}
		nodes.remove(component);
		node.setManager(null);
		orphaned.put(node, null);
		if (node == rootNode) {
			rootNode = null;
		}
	}
	
	public Component getRoot() {
		if (rootNode == null) {
			return null;
		}
		return rootNode.getComponent();
	}

	public Component getParent(Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			return null;
		}
		ComponentNode parentNode = node.getParent();
		if (parentNode == null) {
			return null;
		}
		return parentNode.getComponent();
	}
	
	public void printComponentTree (PrintStream out, Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			return;
		}
		printComponentTree(out, node, 0);
	}
	
	public List<Component> getChildren(Component component) {
		ComponentNode node = nodes.get(component);
		if (node == null) {
			return Collections.emptyList();
		}
		List<Component> res = new ArrayList<Component>();
		for (ComponentNode child: node.getChildren()) {
			res.add(child.getComponent());
		}
		return res;
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
}
