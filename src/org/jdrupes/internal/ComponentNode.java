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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import org.jdrupes.Channel;
import org.jdrupes.Component;
import org.jdrupes.Event;
import org.jdrupes.Manager;
import org.jdrupes.annotation.Handler;

/**
 * ComponentNode is the base class for all nodes in the component tree.
 * ComponentNode is extended by {@link org.jdrupes.AbstractComponent}
 * for the use as base class for component implementations. As an 
 * alternative for implementing components with an independent base class,
 * the derived class {@link org.jdrupes.internal.ComponentProxy} can be
 * used. 
 * 
 * @author mnl
 */
public abstract class ComponentNode implements Manager {

	/** Reference to the common properties of the tree nodes. */
	private ComponentCommon common = null;
	/** Reference to the parent node. */
	private ComponentNode parent = null;
	/** All the node's children */
	private List<ComponentNode> children 
		= Collections.synchronizedList(new ArrayList<ComponentNode>());
	/** The handlers provided by this component. */
	private List<HandlerReference> handlers = new ArrayList<HandlerReference>();
	
	/** The event manager that we delegate to. */
	private ThreadLocal<EventManager> eventManager
		= new ThreadLocal<EventManager>();

	/** 
	 * Initialize the ComponentNode. By default it forms a stand-alone
	 * tree, i.e. the root is set to the component itself.
	 */
	protected ComponentNode() {
		common = new ComponentCommon(this);
	}

	/**
	 * Initialize the handler list of this component. May only be called
	 * when {@link #getComponent()} can be relied on to return the
	 * correct value.
	 */
	protected void initComponentsHandlers() {
		for (Method m : getComponent().getClass().getMethods()) {
			Handler handlerAnnotation = m.getAnnotation(Handler.class);
			if (handlerAnnotation == null) {
				continue;
			}
			List<Object> eventKeys = new ArrayList<Object>();
			if (handlerAnnotation.events()[0] != Handler.NO_EVENT.class) {
				eventKeys.addAll(Arrays.asList(handlerAnnotation.events()));
			}
			if (!handlerAnnotation.namedEvents()[0].equals("")) {
				eventKeys.addAll
					(Arrays.asList(handlerAnnotation.namedEvents()));
			}
			List<Object> channelKeys = new ArrayList<Object>();
			if (handlerAnnotation.channels()[0] != Handler.NO_CHANNEL.class) {
				channelKeys.addAll(Arrays.asList(handlerAnnotation.channels()));
			}
			if (!handlerAnnotation.namedChannels()[0].equals("")) {
				channelKeys.addAll
					(Arrays.asList(handlerAnnotation.namedChannels()));
			}
			if (channelKeys.size() == 0) {
				channelKeys.add(getChannel().getMatchKey());
			}
			for (Object eventKey : eventKeys) {
				for (Object channelKey : channelKeys) {
					handlers.add(new HandlerReference
							(eventKey, channelKey, getComponent(), m));
				}
			}
		}
		handlers = Collections.synchronizedList(handlers);
	}

	/**
	 * Return the component represented by this node in the tree.
	 * 
	 * @return the component
	 */
	protected abstract Component getComponent();

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
	 * Lock the tree that the given node belongs to. This cannot
	 * be done by simply synchronizing on "common" because the
	 * common may no longer be the nodes "common" when we actually
	 * get the lock. 
	 */
	private void lockAndRun (ComponentNode node, Runnable runnable) {
		while (true) {
			ComponentCommon common = node.common;
			synchronized (common) {
				if (node.common != common) {
					continue;
				}
				runnable.run();
				break;
			}
		}
	}
	
	/**
	 * Set the reference to the common properties of this component 
	 * and all its children to the given value.
	 * 
	 * @param comp the new root
	 */
	private void setCommon(ComponentCommon common) {
		this.common = common;
		for (ComponentNode child: children) {
			child.setCommon(common);
		}
	}

	/**
	 * Remove the component from the tree, making it a stand-alone tree.
	 */
	public Component detach() {
		if (parent != null) {
			lockAndRun(this, new Runnable() {
				@Override
				public void run() {
					parent.children.remove(ComponentNode.this);
					parent.common.clearHandlerCache();
					parent = null;
					ComponentCommon newCommon 
						= new ComponentCommon(ComponentNode.this);
					synchronized (newCommon) {
						setCommon(newCommon);
					}
				}
			});
		}
		return getComponent();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getChildren()
	 */
	@Override
	public List<Component> getChildren() {
		List<Component> children = new ArrayList<Component>();
		for (ComponentNode child: this.children) {
			children.add(child.getComponent());
		}
		return Collections.unmodifiableList(children);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getParent()
	 */
	@Override
	public Component getParent() {
		if (parent == null) {
			return null;
		}
		return parent.getComponent();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getRoot()
	 */
	@Override
	public Component getRoot() {
		return common.getRoot().getComponent();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#addChild(Component)
	 */
	@Override
	public Manager addChild (Component child) {
		ComponentNode childNode = getComponentNode(child);
		if (childNode == null) {
			childNode = new ComponentProxy(child);
		}
		final ComponentNode cn = childNode;
		lockAndRun(this, new Runnable() {
			@Override
			public void run() {
				lockAndRun(cn, new Runnable() {
					@Override
					public void run() {
						if (cn.parent != null) {
							cn.parent.children.remove(cn);
						}
						cn.parent = ComponentNode.this;
						cn.setCommon(common);
						children.add(cn);
					}
				});
			}
		});
		common.clearHandlerCache();
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
	
	/**
	 * An iterator for getting all nodes of the tree.
	 */
	private static class TreeIterator implements Iterator<Component> {

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

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.ComponentManager#addHandler
	 */
	@Override
	public void addHandler(Object eventKey, Object channelKey, String method) {
		if (channelKey instanceof Channel) {
			channelKey = ((Matchable)channelKey).getMatchKey();
		}
		try {
			for (Method m: getComponent().getClass().getMethods()) {
				if (m.getName().equals(method)
					&& m.getParameterTypes().length == 1
					&& Event.class.isAssignableFrom(m.getParameterTypes()[0])) {
					handlers.add(new HandlerReference(eventKey, channelKey, 
							getComponent(), m));
					return;
				}
			}
			throw new IllegalArgumentException("No matching method");
		} catch (SecurityException e) {
			throw (RuntimeException)
				(new IllegalArgumentException().initCause(e));
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.internal.EventManager#fire
	 * (org.jdrupes.Event, org.jdrupes.Channel)
	 */
	@Override
	public void fire(Event event, Channel... channel) {
		EventManager em = eventManager.get();
		if (em == null) {
			em = new EventManagerImpl(common);
		}
		em.fire(event, channel);
	}

	/**
	 * @param event
	 * @see org.jdrupes.Manager#fire(org.jdrupes.Event)
	 */
	@Override
	public void fire(Event event) {
		fire(event, getChannel());
	}
	
	/**
	 * Collects all handler. Iterates over the tree with this object
	 * as root and for all components adds the matching handlers to
	 * the result set.
	 * 
	 * @param hdlrs the result set
	 * @param event the event to match
	 * @param channels the channels to match
	 */
	void collectHandlers (Set<HandlerReference> hdlrs, 
			EventBase event, Channel[] channels) {
		for (HandlerReference hdlr: handlers) {
			if (!event.matches(hdlr.getEventKey())) {
				continue;
			}
			// Channel.class as handler's channel matches everything
			boolean match = false;
			for (Channel channel : channels) {
				if (channel.matches(hdlr.getChannelKey())) {
					match = true;
					break;
				}
			}
			if (!match) {
				continue;
			}
			hdlrs.add(hdlr);
		}
		for (ComponentNode child: children) {
			child.collectHandlers(hdlrs, event, channels);
		}
	}
	
}
