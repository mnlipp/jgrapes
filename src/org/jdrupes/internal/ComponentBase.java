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
import org.jdrupes.Manager;

/**
 * @author mnl
 *
 */
public abstract class ComponentBase 
	implements Manager, Iterable<Component> {

	private ComponentBase root = null;
	private ComponentBase parent = null;
	private List<ComponentBase> children = new ArrayList<ComponentBase>();

	protected ComponentBase() {
		root = this;
	}
	
	public abstract Component getComponent();
	
	public Component detach() {
		if (parent != null) {
			parent.children.remove(this);
			parent = null;
			setRoot(this);
		}
		return getComponent();
	}

	private void setRoot(ComponentBase comp) {
		root = comp;
		for (ComponentBase child: children) {
			child.setRoot(comp);
		}
	}
	
	public List<Component> getChildren() {
		List<Component> children = new ArrayList<Component>();
		for (ComponentBase child: this.children) {
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

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Component> iterator() {
		return new TreeIterator(this);
	}
	
	public class TreeIterator implements Iterator<Component> {

		private class Pos {
			public ComponentBase current;
			public int childIndex;
			public Pos(ComponentBase cm) {
				current = cm;
				childIndex = -1;
			}
		}
		
		private Stack<Pos> stack = new Stack<Pos>();
		
		public TreeIterator(ComponentBase root) {
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
			ComponentBase res = pos.current;
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
	
	public Manager addChild (Component child) {
		ComponentBase childBase 
			= (ComponentBase)ComponentManager.getComponentBase(child);
		if (childBase == null) {
			childBase = new ComponentManager(child);
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
		ComponentBase childBase 
			= (ComponentBase)ComponentManager.getComponentBase(child);
		childBase.detach();
	}

}
