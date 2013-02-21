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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.jdrupes.Component;
import org.jdrupes.Event;
import org.jdrupes.Manager;

/**
 * ComponentNode is the base class for all nodes in the tree of components.
 * Actual components either extend the ComponentNode or are referenced
 * by a ComponentProxy that extends the ComponentNode. 
 * 
 * @author mnl
 */
public abstract class ComponentNode implements Manager {

	/** Reference to the root node of the tree. */
	private ComponentNode root = null;
	/** Reference to the parent node. */
	private ComponentNode parent = null;
	/** All the node's children */
	private List<ComponentNode> children = new ArrayList<ComponentNode>();
	/** The event manager that we delegate to. */
	private ThreadLocal<EventManager> eventManager;

	/** 
	 * Initialize the ComponentNode. By default it forms a stand-alone
	 * tree, i.e. the root is set to the component itself.
	 */
	protected ComponentNode() {
		setRoot(this);
	}

	/**
	 * Return the component represented by this node in the tree.
	 * 
	 * @return the component
	 */
	public abstract Component getComponent();

	/**
	 * Return the component node for a given component.
	 * 
	 * @param component the component
	 * @return the node representing the component in the tree
	 */
	public static ComponentNode getComponentNode (Component component) {
		if (component instanceof ComponentNode) {
			return (ComponentNode)component;
		}
		return ComponentProxy.getComponentProxy(component);
	}

	/**
	 * Remove the component from the tree, making it a stand-alone tree.
	 */
	public Component detach() {
		if (parent != null) {
			parent.children.remove(this);
			parent = null;
			setRoot(this); // implies setting children's root
		}
		return getComponent();
	}

	/**
	 * Set the root of this component and all its children to the
	 * given component.
	 * 
	 * @param comp the new root
	 */
	private void setRoot(ComponentNode comp) {
		root = comp;
		for (ComponentNode child: children) {
			child.setRoot(comp);
		}
	}

	public List<Component> getChildren() {
		List<Component> children = new ArrayList<Component>();
		for (ComponentNode child: this.children) {
			children.add(child.getComponent());
		}
		return Collections.unmodifiableList(children);
	}

	public Component getParent() {
		if (parent == null) {
			return null;
		}
		return parent.getComponent();
	}
	
	public Component getRoot() {
		return root.getComponent();
	}

	public Manager addChild (Component child) {
		ComponentNode childBase = getComponentNode(child);
		if (childBase == null) {
			childBase = new ComponentProxy(child);
		}
		childBase.detach();
		children.add(childBase);
		childBase.parent = this;
		childBase.setRoot(root);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#removeChild(Component child)
	 */
	@Override
	public void removeChild(Component child) {
		ComponentNode childBase = getComponentNode(child);
		childBase.detach();
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Component> iterator() {
		return new TreeIterator(this);
	}
	
	public class TreeIterator implements Iterator<Component> {

		private class Pos {
			public ComponentNode current;
			public int childIndex;
			public Pos(ComponentNode cm) {
				current = cm;
				childIndex = -1;
			}
		}
		
		private Stack<Pos> stack = new Stack<Pos>();
		
		public TreeIterator(ComponentNode root) {
			stack.push(new Pos(root));
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return !stack.empty();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Component next() {
			if (stack.empty()) {
				throw new NoSuchElementException();
			}
			Pos pos = stack.peek();
			ComponentNode res = pos.current;
			while (true) {
				if (pos.current.children.size() > ++pos.childIndex) {
					stack.push(new Pos
							   (pos.current.children.get(pos.childIndex)));
					return res.getComponent();
				}
				stack.pop();
				if (stack.empty()) {
					break;
				}
				pos = stack.peek();	
			}
			return res.getComponent();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * @param event
	 * @see org.jdrupes.internal.EventManager#fire(org.jdrupes.Event)
	 */
	public void fire(Event event) {
		eventManager.get().fire(event);
	}
}
