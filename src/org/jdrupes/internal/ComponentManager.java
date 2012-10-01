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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.jdrupes.Component;
import org.jdrupes.Manager;

/**
 * @author mnl
 *
 */
public class ComponentManager implements Manager {

	private Component component = null;
	private ComponentManager root = null;
	private ComponentManager parent = null;
	private List<ComponentManager> children = new ArrayList<ComponentManager>();
	
	public ComponentManager(Component component) {
		this.component = component;
		this.root = this;
		try {
			Field field = component.getClass().getDeclaredField("manager");
			if (!field.isAccessible()) {
				field.setAccessible(true);
				field.set(component, this);
				field.setAccessible(false);
			} else {
				field.set(component, this);
			}
		} catch (SecurityException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException
				("Components must have a manager attribute");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		}
	}

	public static Manager getManager (Component component) {
		Manager manager = null;
		try {
			Field field = component.getClass().getDeclaredField("manager");
			if (!field.isAccessible()) {
				field.setAccessible(true);
				manager = (Manager)field.get(component);
				field.setAccessible(false);
			} else {
				manager = (Manager)field.get(component);
			}
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		return manager;
	}

	private void removeChild(ComponentManager cm) {
		children.remove(cm);
	}
	
	public Component getComponent() {
		return component;
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#detach()
	 */
	@Override
	public Component detach() {
		if (parent != null) {
			parent.removeChild(this);
			parent = null;
			setRoot(this);
		}
		return component;
	}

	private void setRoot(ComponentManager mgr) {
		root = mgr;
		for (ComponentManager child: children) {
			child.setRoot(mgr);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#addChild(Component child)
	 */
	@Override
	public Manager addChild (Component child) {
		ComponentManager childMgr = (ComponentManager)getManager(child);
		if (childMgr == null) {
			childMgr = new ComponentManager(child);
		} else {
			childMgr.detach();
		}
		children.add(childMgr);
		childMgr.parent = this;
		childMgr.setRoot(root);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#removeChild(Component child)
	 */
	@Override
	public void removeChild(Component child) {
		ComponentManager childMgr = (ComponentManager)getManager(child);
		childMgr.detach();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getChildren()
	 */
	@Override
	public List<Component> getChildren() {
		List<Component> children = new ArrayList<Component>();
		for (ComponentManager childMgr: this.children) {
			children.add(childMgr.getComponent());
		}
		return Collections.unmodifiableList(children);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getParent()
	 */
	public Component getParent() {
		if (parent == null) {
			return null;
		}
		return parent.getComponent();
	}
	
	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getRoot()
	 */
	public Component getRoot() {
		return root.getComponent();
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
			public ComponentManager current;
			public int childIndex;
			public Pos(ComponentManager cm) {
				current = cm;
				childIndex = -1;
			}
		}
		
		private Stack<Pos> stack = new Stack<Pos>();
		
		public TreeIterator(ComponentManager root) {
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
			Component res = pos.current.getComponent();
			while (true) {
				if (pos.current.children.size() > ++pos.childIndex) {
					stack.push(new Pos(pos.current.children.get(pos.childIndex)));
					return res;
				}
				stack.pop();
				if (stack.empty()) {
					break;
				}
				pos = stack.peek();	
			}
			return res;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
