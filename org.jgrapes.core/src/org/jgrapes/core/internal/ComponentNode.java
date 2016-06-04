/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrapes.core.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Attached;
import org.jgrapes.core.events.Detached;

/**
 * ComponentNode is the base class for all nodes in the component tree.
 * ComponentNode is extended by {@link org.jgrapes.core.AbstractComponent}
 * for the use as base class for component implementations. As an 
 * alternative for implementing components with an independent base class,
 * the derived class {@link org.jgrapes.core.internal.ComponentProxy} can be
 * used. 
 * 
 * @author Michael N. Lipp
 */
public abstract class ComponentNode implements Manager {

	/** Reference to the common properties of the tree nodes. */
	private ComponentTree tree = null;
	/** Reference to the parent node. */
	private ComponentNode parent = null;
	/** All the node's children */
	private List<ComponentNode> children = new ArrayList<>();
	/** The handlers provided by this component. */
	private List<HandlerReference> handlers = new ArrayList<HandlerReference>();
	
	/** 
	 * Initialize the ComponentNode. By default it forms a stand-alone
	 * tree, i.e. the root is set to the component itself.
	 */
	protected ComponentNode() {
	}

	/**
	 * Initialize the handler list of this component. May only be called
	 * when {@link #getComponent()} can be relied on to return the
	 * correct value.
	 */
	protected void initComponentsHandlers() {
		// Have a look at all methods.
		for (Method m : getComponent().getClass().getMethods()) {
			Handler handlerAnnotation = m.getAnnotation(Handler.class);
			// Methods without handler annotation are ignored
			if (handlerAnnotation == null) {
				continue;
			}
			// Get all event keys from the handler annotation.
			List<Object> eventKeys = new ArrayList<Object>();
			if (handlerAnnotation.events()[0] != Handler.NO_EVENT.class) {
				eventKeys.addAll(Arrays.asList(handlerAnnotation.events()));
			}
			// Get all named events from the annotation and add to event keys.
			if (!handlerAnnotation.namedEvents()[0].equals("")) {
				eventKeys.addAll
					(Arrays.asList(handlerAnnotation.namedEvents()));
			}
			// If no event types are given, try first parameter.
			if (eventKeys.isEmpty()) {
				Class<?>[] paramTypes = m.getParameterTypes();
				if (paramTypes.length > 0) {
					if (Event.class.isAssignableFrom(paramTypes[0])) {
						eventKeys.add(paramTypes[0]);
					}
				}
			}
			// Get channel keys from the annotation.
			List<Object> channelKeys = new ArrayList<Object>();
			if (handlerAnnotation.channels()[0] != Handler.NO_CHANNEL.class) {
				channelKeys.addAll(Arrays.asList(handlerAnnotation.channels()));
			}
			// Get named channels from annotation and add to channel keys.
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
							(eventKey, channelKey, getComponent(), m,
							 handlerAnnotation.priority()));
				}
			}
		}
		handlers = Collections.synchronizedList(handlers);
	}

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
	 * Returns the component represented by this node in the tree.
	 * 
	 * @return the component
	 */
	protected abstract Component getComponent();

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getChildren()
	 */
	@Override
	synchronized public List<Component> getChildren() {
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
	synchronized public Component getParent() {
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
		return getTree().getRoot().getComponent();
	}

	/**
	 * Return the tree that this node belongs to. If the node does not
	 * belong to a tree yet, a tree is automatically created.
	 * 
	 * @return the tree
	 */
	ComponentTree getTree() {
		if (tree != null) {
			return tree;
		}
		tree = new ComponentTree(this);
		tree.setEventPipeline(new EventBuffer(tree));
		fire(new Attached(getComponent(), null), getChannel());
		return tree;
	}
	
	/**
	 * Set the reference to the common properties of this component 
	 * and all its children to the given value.
	 * 
	 * @param comp the new root
	 */
	synchronized private void setTree(ComponentTree tree) {
		this.tree = tree;
		for (ComponentNode child: children) {
			child.setTree(tree);
		}
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#addChild(Component)
	 */
	@Override
	synchronized public Manager attach (Component child) {
		ComponentNode childNode = getComponentNode(child);
		synchronized (childNode) {
			synchronized (getTree()) {
				if (childNode.tree == null) { 
					// Newly created, stand-alone child node
					childNode.parent = ComponentNode.this;
					childNode.setTree(tree);
					children.add(childNode);
				} else {
					// Attaching a tree...
					synchronized (childNode.tree) {
						if (childNode.parent != null) {
							throw new IllegalStateException
								("Cannot attach a node with a parent.");
						}
						if (childNode.tree.isStarted()) {
							throw new IllegalStateException
								("Cannot attach a started subtree.");
						}
						childNode.parent = ComponentNode.this;
						ComponentTree childTree = childNode.tree;
						childNode.setTree(tree);
						children.add(childNode);
						tree.mergeEvents(childTree);
					}
				}
			}
		}
		Channel pChan = getChannel();
		if (pChan == null) {
			pChan = Channel.BROADCAST;
		}
		Channel cChan = childNode.getChannel();
		if (cChan == null) {
			pChan = Channel.BROADCAST;
		}
		Event e = new Attached(childNode.getComponent(), getComponent());
		if (pChan.equals(Channel.BROADCAST) 
			|| cChan.equals(Channel.BROADCAST)) {
			fire(e, Channel.BROADCAST);
		} else if (pChan.equals(cChan)) {
			fire(e, pChan);
		} else {
			fire(e, pChan, cChan);
		}
		return this;
	}
	
	/**
	 * Remove the component from the tree, making it a stand-alone tree.
	 */
	synchronized public Component detach() {
		if (parent != null) {
			ComponentNode oldParent = parent;
			synchronized (tree) {
				if (!tree.isStarted()) {
					throw new IllegalStateException
						("Components may not be detached from a tree before"
						 + " a Start event has been fired on it.");
				}
				synchronized (oldParent) {
					parent.children.remove(ComponentNode.this);
					parent.tree.clearHandlerCache();
					parent = null;
				}
				ComponentTree newTree 
					= new ComponentTree(ComponentNode.this);
				newTree.setEventPipeline(new EventProcessor(newTree));
				setTree(newTree);
			}
			Event e = new Detached(getComponent(), oldParent.getComponent());
			oldParent.fire(e);
			e = new Detached(getComponent(), oldParent.getComponent());
			fire(e);
		}
		return getComponent();
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
			public Iterator<ComponentNode> childIter;
			public Pos(ComponentNode cm) {
				current = cm;
				childIter = current.children.iterator();
			}
		}
		
		private Stack<Pos> stack = new Stack<Pos>();
		private ComponentTree tree;
		
		public TreeIterator(ComponentNode root) {
			tree = root.getTree();
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
				synchronized (pos.current) {
					if (pos.current.tree != tree) {
						throw new ConcurrentModificationException();
					}
					if (pos.childIter.hasNext()) {
						stack.push(new Pos(pos.childIter.next()));
						break;
					}
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
	 * @see org.jdrupes.Manager#addHandler
	 */
	@Override
	public void addHandler(Object eventKey, Object channelKey, 
			String method, int priority) {
		if (channelKey instanceof Channel) {
			channelKey = ((Matchable)channelKey).getMatchKey();
		}
		try {
			for (Method m: getComponent().getClass().getMethods()) {
				if (m.getName().equals(method)
					&& m.getParameterTypes().length == 1
					&& Event.class.isAssignableFrom(m.getParameterTypes()[0])) {
					handlers.add(new HandlerReference(eventKey, channelKey, 
							getComponent(), m, priority));
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
	 * @see org.jdrupes.Manager#addHandler
	 */
	@Override
	public void addHandler(Object eventKey, Object channelKey, String method) {
		addHandler(eventKey, channelKey, method, 0);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.core.Manager#fire
	 * (org.jdrupes.Event, org.jdrupes.Channel)
	 */
	@Override
	public void fire(Event event, Channel... channels) {
		if (channels.length == 0) {
			channels = event.getChannels();
			if (channels == null || channels.length == 0) {
				channels = new Channel[] { getChannel() };
			}
		}
		event.setChannels(channels);
		getTree().fire(event, channels);
	}

	/**
	 * Collects all handlers. Iterates over the tree with this object
	 * as root and for all child components adds the matching handlers to
	 * the result set recursively.
	 * 
	 * @param hdlrs the result set
	 * @param event the event to match
	 * @param channels the channels to match
	 */
	void collectHandlers (Collection<HandlerReference> hdlrs, 
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

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#newSynchronousPipeline()
	 */
	@Override
	public EventPipeline newSyncEventPipeline() {
		return new CheckingPipelineFilter
				(new SynchronousEventProcessor(getTree()));
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#newEventPipeline()
	 */
	@Override
	public EventPipeline newEventPipeline() {
		return new CheckingPipelineFilter(new EventProcessor(getTree()));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		return builder.toString();
	}
	
	
}
