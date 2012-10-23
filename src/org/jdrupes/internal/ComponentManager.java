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

import org.jdrupes.Component;
import org.jdrupes.Manager;

/**
 * @author mnl
 *
 */
public class ComponentManager extends ComponentBase {

	private Component component = null;
	
	private static Field getManagerField(Class<?> clazz) {
		try {
			while (true) {
				for (Field field: clazz.getDeclaredFields()) {
					if (field.getAnnotation(Manager.Slot.class) != null) {
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
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		}
	}
	
	public ComponentManager(Component component) {
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
		} catch (SecurityException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException
				("Cannot access component's manager attribute");
		}
	}

	public static ComponentBase getComponentBase (Component component) {
		if (component instanceof ComponentBase) {
			return (ComponentBase)component;
		}
		ComponentManager componentBase = null;
		try {
			Field field = getManagerField(component.getClass());
			if (!field.isAccessible()) {
				field.setAccessible(true);
				componentBase = (ComponentManager)field.get(component);
				field.setAccessible(false);
			} else {
				componentBase = (ComponentManager)field.get(component);
			}
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		return componentBase;
	}

	public Component getComponent() {
		return component;
	}
	
}
