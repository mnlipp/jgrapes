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

/**
 * This interface marks a class as a component. Implementing this 
 * interface is an alternative to deriving a component from 
 * {@link AbstractComponent} (usually because there is some other
 * preferential inheritance relationship). Components aren't required to
 * implement specific methods. They must, however, declare a field
 * for an associated manager. This field must be of type 
 * {@link Manager} and annotated as {@link Manager.Slot}.
 * 
 * The implementation of the component can use the value in the field
 * to get access to the component hierarchy. The field is initialized
 * when the component is added to the component hierarchy or when
 * calling {@link Utils#ensureManager(Component)}.
 */
public interface Component {	
}
