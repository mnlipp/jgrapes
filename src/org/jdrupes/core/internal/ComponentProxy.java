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
package org.jdrupes.core.internal;

import java.lang.reflect.Field;

import org.jdrupes.core.Channel;
import org.jdrupes.core.Component;
import org.jdrupes.core.NamedChannel;
import org.jdrupes.core.annotation.ComponentManager;
import org.jdrupes.core.annotation.Handler;

/**
 * The ComponentProxy is a special ComponentNode that references the
 * object implementing the Component interface (instead of being
 * its base class).
 * 
 * @author mnl
 */
public class ComponentProxy extends ComponentNode {

	/** The reference to the actual component. */
	private Component component = null;
	/** The referenced component's channel. */
	private Channel componentChannel = Channel.BROADCAST;
	
	private static Field getManagerField(Class<?> clazz) {
		try {
			while (true) {
				for (Field field: clazz.getDeclaredFields()) {
					if (field.getAnnotation(ComponentManager.class) != null) {
						return field;
					}
				}
				clazz = clazz.getSuperclass();
				if (clazz == null) {
					throw new IllegalArgumentException
						("Components must have a manager attribute");
				}
			}
		} catch (SecurityException e) {
			throw (RuntimeException)(new IllegalArgumentException
				("Cannot access component's manager attribute")).initCause(e);
		}
	}

	private static Channel getComponentChannel(Field field) {
		ComponentManager cma = field.getAnnotation(ComponentManager.class);
		if (cma.channel() != Handler.NO_CHANNEL.class) {
			if (cma.channel() != Channel.BROADCAST.getMatchKey()) {
				try {
					return cma.channel().newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
				}
			}
			return Channel.BROADCAST;
		}
		if (!cma.namedChannel().equals("")) {
			return new NamedChannel(cma.namedChannel());
		}
		return Channel.BROADCAST;
	}
	
	public ComponentProxy(Component component) {
		this.component = component;
		try {
			Field field = getManagerField(component.getClass());
			if (!field.isAccessible()) {
				field.setAccessible(true);
				field.set(component, this);
				field.setAccessible(false);
			} else {
				field.set(component, this);
			}
			componentChannel = getComponentChannel(field);
			initComponentsHandlers();
		} catch (SecurityException | IllegalAccessException e) {
			throw (RuntimeException)(new IllegalArgumentException
				("Cannot access component's manager attribute")).initCause(e);
		}
	}

	/**
	 * Return the component node for a component that is represented
	 * by a proxy in the tree.
	 * 
	 * @param component the component
	 * @return the node representing the component in the tree
	 */
	static ComponentNode getComponentProxy (Component component) {
		ComponentProxy componentProxy = null;
		try {
			Field field = getManagerField(component.getClass());
			if (!field.isAccessible()) {
				field.setAccessible(true);
				componentProxy = (ComponentProxy)field.get(component);
				field.setAccessible(false);
			} else {
				componentProxy = (ComponentProxy)field.get(component);
			}
		} catch (SecurityException | IllegalAccessException e) {
			throw (RuntimeException)(new IllegalArgumentException
				("Cannot access component's manager attribute")).initCause(e);
		}
		return componentProxy;
	}

	public Component getComponent() {
		return component;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.Manager#getChannel()
	 */
	@Override
	public Channel getChannel() {
		return componentChannel;
	}
}
