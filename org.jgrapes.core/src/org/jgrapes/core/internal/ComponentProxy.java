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

import java.lang.reflect.Field;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Manager;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.annotation.ComponentManager;
import org.jgrapes.core.annotation.Handler;

/**
 * The ComponentProxy is a special ComponentVertex that references the
 * object implementing the Component interface (instead of being
 * its base class).
 */
public class ComponentProxy extends ComponentVertex {

	/** The reference to the actual component. */
	private ComponentType component = null;
	/** The referenced component's channel. */
	private Channel componentChannel = Channel.BROADCAST;
	
	private static Field getManagerField(Class<?> clazz) {
		try {
			while (true) {
				for (Field field: clazz.getDeclaredFields()) {
					if (Manager.class.isAssignableFrom(field.getType())
							&& field.getAnnotation(ComponentManager.class) != null) {
						return field;
					}
				}
				clazz = clazz.getSuperclass();
				if (clazz == null) {
					throw new IllegalArgumentException(
							"Components must have a manager attribute");
				}
			}
		} catch (SecurityException e) {
			throw (RuntimeException)(new IllegalArgumentException(
					"Cannot access component's manager attribute")).initCause(e);
		}
	}

	private static Channel getComponentChannel(Field field) {
		ComponentManager cma = field.getAnnotation(ComponentManager.class);
		if (cma.channel() != Handler.NoChannel.class) {
			if (cma.channel() != Channel.BROADCAST.defaultCriterion()) {
				try {
					return cma.channel().newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					// Ignored
				}
			}
			return Channel.BROADCAST;
		}
		if (!cma.namedChannel().equals("")) {
			return new NamedChannel(cma.namedChannel());
		}
		return Channel.SELF;
	}
	
	/**
	 * Create a new component proxy for the component and assign it to 
	 * the specified field which must be of type {@link Manager}.
	 * 
	 * @param field the field that gets the proxy assigned
	 * @param componentChannel the componen't channel
	 * @param component the component
	 */
	private ComponentProxy(
			Field field, ComponentType component, Channel componentChannel) {
		this.component = component;
		try {
			field.set(component, this);
			if (componentChannel == null) {
				componentChannel = getComponentChannel(field);
			}
			if (componentChannel.equals(Channel.SELF)) {
				componentChannel = this;
			}
			this.componentChannel = componentChannel;
			initComponentsHandlers();
		} catch (SecurityException | IllegalAccessException e) {
			throw (RuntimeException)(new IllegalArgumentException(
					"Cannot access component's manager attribute")).initCause(e);
		}
	}

	/**
	 * Return the component node for a component that is represented
	 * by a proxy in the tree.
	 * 
	 * @param component the component
	 * @param componentChannel the component's channel
	 * @return the node representing the component in the tree
	 */
	static ComponentVertex getComponentProxy(
			ComponentType component, Channel componentChannel) {
		ComponentProxy componentProxy = null;
		try {
			Field field = getManagerField(component.getClass());
			synchronized (component) {
				if (!field.isAccessible()) {
					field.setAccessible(true);
					componentProxy = (ComponentProxy)field.get(component);
					if (componentProxy == null) {
						componentProxy = new ComponentProxy(
								field, component, componentChannel);
					}
					field.setAccessible(false);
				} else {
					componentProxy = (ComponentProxy)field.get(component);
					if (componentProxy == null) {
						componentProxy = new ComponentProxy(
								field, component, componentChannel);
					}
				}
			}
		} catch (SecurityException | IllegalAccessException e) {
			throw (RuntimeException)(new IllegalArgumentException(
					"Cannot access component's manager attribute")).initCause(e);
		}
		return componentProxy;
	}

	public ComponentType component() {
		return component;
	}

	/* (non-Javadoc)
	 * @see org.jgrapes.core.Manager#getChannel()
	 */
	@Override
	public Channel channel() {
		return componentChannel;
	}

	/**
	 * Return the object itself as value.
	 */
	@Override
	public Object defaultCriterion() {
		return this;
	}

	/**
	 * Matches the object itself (using identity comparison) or the
	 * {@link Channel} class.
	 * 
	 * @see Channel#isEligibleFor(Object)
	 */
	@Override
	public boolean isEligibleFor(Object value) {
		return value.equals(Channel.class) 
				|| value == defaultCriterion();
	}
}
