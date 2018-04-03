/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.ExecutorService;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.HandlerScope;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.HandlerDefinition;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.events.Attached;
import org.jgrapes.core.events.Detached;
import org.jgrapes.core.events.Start;

/**
 * ComponentVertex is the base class for all nodes in the component tree.
 * ComponentVertex is extended by {@link org.jgrapes.core.Component}
 * for the use as base class for component implementations. As an 
 * alternative for implementing components with an independent base class,
 * the derived class {@link org.jgrapes.core.internal.ComponentProxy} can be
 * used. 
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis",
        "PMD.AvoidDuplicateLiterals" })
public abstract class ComponentVertex implements Manager, Channel {

	/** The component's (optional) name. */
	private String name;
	/** Reference to the common properties of the tree nodes. */
	private ComponentTree tree;
	/** Reference to the parent node. */
	private ComponentVertex parent;
	/** All the node's children */
	private final List<ComponentVertex> children = new ArrayList<>();
	/** The handlers provided by this component. */
	private List<HandlerReference> handlers;
	
	/** 
	 * Initialize the ComponentVertex. By default it forms a stand-alone
	 * tree, i.e. the root is set to the component itself.
	 */
	protected ComponentVertex() {
		// Nothing to do, but appropriate for abstract class.
	}

	/**
	 * Initialize the handler list of this component. May only be called
	 * when {@link #component()} can be relied on to return the
	 * correct value.
	 */
	@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
	        "PMD.AvoidDuplicateLiterals" })
	protected void initComponentsHandlers(
			ChannelReplacements channelReplacements) {
		handlers = new ArrayList<HandlerReference>();
		// Have a look at all methods.
		for (Method m : component().getClass().getMethods()) {
			maybeAddHandler(m, channelReplacements);
		}
		handlers = Collections.synchronizedList(handlers);
	}

	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	private void maybeAddHandler(
			Method method, ChannelReplacements channelReplacements) {
		for (Annotation annotation: method.getDeclaredAnnotations()) {
			Class<?> annoType = annotation.annotationType();
			HandlerDefinition hda = annoType.getAnnotation(
					HandlerDefinition.class);
			if (hda == null) {
				continue;
			}
			HandlerDefinition.Evaluator evaluator 
				= Common.definitionEvaluator(hda);
			HandlerScope scope = evaluator.scope(component(), method, 
					channelReplacements);
			if (scope == null) {
				continue;
			}
			handlers.add(HandlerReference.newRef(
					component(), method, evaluator.priority(annotation),
							scope));
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#setName(java.lang.String)
	 */
	@Override
	public ComponentType setName(String name) {
		this.name = name;
		return component();
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#name()
	 */
	@Override
	public String name() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#path()
	 */
	@Override
	public String componentPath() {
		StringBuilder res = new StringBuilder();
		buildPath(res);
		return res.toString();
	}

	private void buildPath(StringBuilder builder) {
		if (parent != null) {
			parent.buildPath(builder);
		}
		builder.append('/')
			.append(name == null ? getClass().getSimpleName() : name);
	}
	
	/**
	 * Return the component node for a given component.
	 * 
	 * @param component the component
	 * @param componentChannel the component's channel
	 * @return the node representing the component in the tree
	 */
	public static ComponentVertex componentVertex(
			ComponentType component, Channel componentChannel) {
		if (component instanceof ComponentVertex) {
			return (ComponentVertex)component;
		}
		return ComponentProxy.getComponentProxy(component, componentChannel);
	}

	/**
	 * Returns the component represented by this node in the tree.
	 * 
	 * @return the component
	 */
	protected abstract ComponentType component();

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#getChildren()
	 */
	@Override
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public List<ComponentType> children() {
		synchronized(this) {
			List<ComponentType> children = new ArrayList<ComponentType>();
			for (ComponentVertex child: this.children) {
				children.add(child.component());
			}
			return Collections.unmodifiableList(children);
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#getParent()
	 */
	@Override
	public ComponentType parent() {
		synchronized (this) {
			if (parent == null) {
				return null;
			}
			return parent.component();
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#getRoot()
	 */
	@Override
	public ComponentType root() {
		return tree().root().component();
	}

	/**
	 * Return the tree that this node belongs to. If the node does not
	 * belong to a tree yet, a tree is automatically created.
	 * 
	 * @return the tree
	 */
	/* default */ ComponentTree tree() {
		if (tree != null) {
			return tree;
		}
		// Build complete tree before assigning it.
		ComponentTree newTree = new ComponentTree(this);
		newTree.setEventPipeline(new BufferingEventPipeline(newTree));
		tree = newTree;
		fire(new Attached(component(), null), channel());
		return tree;
	}
	
	/**
	 * Set the reference to the common properties of this component 
	 * and all its children to the given value.
	 * 
	 * @param comp the new root
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	private void setTree(ComponentTree tree) {
		synchronized (this) {
			this.tree = tree;
			for (ComponentVertex child: children) {
				child.setTree(tree);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#attach(Component)
	 */
	@Override
	@SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
	        "PMD.CyclomaticComplexity", "PMD.NcssCount",
	        "PMD.NPathComplexity" })
	public <T extends ComponentType> T attach(T child) {
		synchronized (this) {
			ComponentVertex childNode = componentVertex(child, null);
			List<Channel> attachedAsChannels = new ArrayList<>();
			synchronized (childNode) {
				if (tree != null && tree.isStarted()) {
					for (TreeIterator itr = new TreeIterator(childNode);
							itr.hasNext();) {
						attachedAsChannels.add(itr.next());
					}
				}
				synchronized (tree()) {
					if (childNode.tree == null) { 
						// Newly created, stand-alone child node
						childNode.parent = this;
						childNode.setTree(tree);
						children.add(childNode);
					} else {
						// Attaching a tree...
						synchronized (childNode.tree) {
							if (childNode.parent != null) {
								throw new IllegalStateException(
										"Cannot attach a node with a parent.");
							}
							if (childNode.tree.isStarted()) {
								throw new IllegalStateException(
										"Cannot attach a started subtree.");
							}
							childNode.parent = this;
							ComponentTree childTree = childNode.tree;
							childNode.setTree(tree);
							children.add(childNode);
							tree.mergeEvents(childTree);
						}
					}
					tree.clearHandlerCache();
				}
			}
			Channel parentChan = channel();
			if (parentChan == null) {
				parentChan = Channel.BROADCAST;
			}
			Channel childChan = childNode.channel();
			if (childChan == null) {
				parentChan = Channel.BROADCAST;
			}
			Attached evt = new Attached(childNode.component(), component());
			if (parentChan.equals(Channel.BROADCAST) 
					|| childChan.equals(Channel.BROADCAST)) {
				fire(evt, Channel.BROADCAST);
			} else if (parentChan.equals(childChan)) {
				fire(evt, parentChan);
			} else {
				fire(evt, parentChan, childChan);
			}
			if (!attachedAsChannels.isEmpty()) {
				fire(new Start(), attachedAsChannels.toArray(new Channel[0]));
			}
			return child;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#detach()
	 */
	@Override
	public ComponentType detach() {
		synchronized (this) {
			if (parent != null) {
				ComponentVertex oldParent = parent;
				synchronized (tree) {
					if (!tree.isStarted()) {
						throw new IllegalStateException(
						        "Components may not be detached from a tree before"
						                + " a Start event has been fired on it.");
					}
					synchronized (oldParent) {
						parent.children.remove(this);
						parent.tree.clearHandlerCache();
						parent = null;
					}
					ComponentTree newTree = new ComponentTree(this);
					newTree.setEventPipeline(new FeedBackPipelineFilter(
					        new EventProcessor(newTree)));
					setTree(newTree);
				}
				Detached evt = new Detached(component(), oldParent.component());
				oldParent.fire(evt);
				evt = new Detached(component(), oldParent.component());
				fire(evt);
			}
			return component();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ComponentType> iterator() {
		return new ComponentIterator(new TreeIterator(this));
	}

	/**
	 * A simple wrapper that converts a component vertex iterator
	 * to a component (type) iterator.
	 */
	private static class ComponentIterator implements Iterator<ComponentType> {

		private final TreeIterator baseIterator;
		
		/**
		 * @param baseIterator
		 */
		public ComponentIterator(TreeIterator baseIterator) {
			this.baseIterator = baseIterator;
		}

		@Override
		public boolean hasNext() {
			return baseIterator.hasNext();
		}

		@Override
		public ComponentType next() {
			return baseIterator.next().component();
		}
		
	}
	
	/**
	 * An iterator for getting all nodes of the tree.
	 */
	private static class TreeIterator implements Iterator<ComponentVertex> {

		private final Stack<CurPos> stack = new Stack<CurPos>();
		private final ComponentTree tree;
		
		/**
		 * The current position.
		 */
		private class CurPos {
			public ComponentVertex current;
			public Iterator<ComponentVertex> childIter;
			
			/**
			 * Instantiates a new current position
			 *
			 * @param vertex the cm
			 */
			public CurPos(ComponentVertex vertex) {
				current = vertex;
				childIter = current.children.iterator();
			}
		}
		
		/**
		 * Instantiates a new tree iterator.
		 *
		 * @param root the root
		 */
		public TreeIterator(ComponentVertex root) {
			tree = root.tree();
			stack.push(new CurPos(root));
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
		@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
		public ComponentVertex next() {
			if (stack.empty()) {
				throw new NoSuchElementException();
			}
			CurPos pos = stack.peek();
			@SuppressWarnings("PMD.PrematureDeclaration")
			ComponentVertex res = pos.current;
			while (true) {
				synchronized (pos.current) {
					if (pos.current.tree != tree) {
						throw new ConcurrentModificationException();
					}
					if (pos.childIter.hasNext()) {
						stack.push(new CurPos(pos.childIter.next()));
						break;
					}
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

	@Override
	public void addHandler(Method method, HandlerScope scope, int priority) {
		handlers.add(HandlerReference.newRef(component(),
		        method, priority, scope));
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.core.Manager#fire
	 * (org.jgrapes.core.Event, org.jgrapes.core.Channel)
	 */
	@Override
	public <T> Event<T> fire(Event<T> event, Channel... channels) {
		if (channels.length == 0) {
			channels = event.channels();
			if (channels.length == 0) {
				channels = new Channel[] { channel() };
			}
		}
		event.setChannels(channels);
		tree().fire(event, channels);
		return event;
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
	@SuppressWarnings("PMD.UseVarargs")
	/* default */ void collectHandlers(Collection<HandlerReference> hdlrs, 
			EventBase<?> event, Channel[] channels) {
		for (HandlerReference hdlr: handlers) {
			if (hdlr.handles(event, channels)) {
				hdlrs.add(hdlr);
			}
		}
		for (ComponentVertex child: children) {
			child.collectHandlers(hdlrs, event, channels);
		}
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#activeEventPipeline()
	 */
	@Override
	public EventPipeline activeEventPipeline() {
		return new CheckingPipelineFilter(
				tree().getEventPipeline(), channel());
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#newSynchronousPipeline()
	 */
	@Override
	public EventPipeline newSyncEventPipeline() {
		return new CheckingPipelineFilter(
				new SynchronousEventProcessor(tree()), channel());
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#newEventPipeline()
	 */
	@Override
	public EventPipeline newEventPipeline() {
		return new CheckingPipelineFilter(new EventProcessor(tree()),
		        channel());
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#newEventPipeline(java.util.concurrent.ExecutorService)
	 */
	@Override
	public EventPipeline newEventPipeline(ExecutorService executorService) {
		return new CheckingPipelineFilter(
				new EventProcessor(tree(), executorService), channel());
	}

	/**
	 * If a name has been set for this component 
	 * (see {@link Manager#setName(String)}), return the name,
	 * else return the object name provided by 
	 * {@link Components#objectName(Object)}, using
	 * {@link #component()} as argument.
	 */
	@Override
	public String toString() {
		if (name != null) {
			return name;
		}
		return Components.objectName(component());
	}
	
	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#registerAsGenerator()
	 */
	@Override
	public void registerAsGenerator() {
		GeneratorRegistry.instance().add(component());
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#unregisterAsGenerator()
	 */
	@Override
	public void unregisterAsGenerator() {
		GeneratorRegistry.instance().remove(component());
	}
	
}
