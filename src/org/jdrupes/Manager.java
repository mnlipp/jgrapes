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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdrupes.internal.ComponentManager;
import org.jdrupes.internal.EventManager;

/**
 * The manager interface provides methods for manipulating the
 * component hierarchy and for firing events. 
 */
public interface Manager extends ComponentManager, EventManager {

	/**
	 * This annotation marks a component's attribute as a slot for the
	 * manager automatically associated with the component 
	 * (see {@link Component}).  
	 */
	@Documented
	@Retention(value=RetentionPolicy.RUNTIME)
	@Target(value=ElementType.FIELD)
	public @interface Slot {
	}
	
}
