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
package org.jdrupes.test.basic;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.jdrupes.Component;
import org.jdrupes.Manager;
import org.junit.Test;

public class StructureTest {

	private Component subtree1(Manager manager, int offset) {
		Component c = new TestComponent("node " + offset);
		Component c1 = new TestComponent("node " + (offset + 1));
		Component c2 = new TestComponent("node " + (offset + 2));
		manager.attach(c, c1);
		manager.attach(c, c2);
		return c;
	}
	
	@Test
	public void testRoot() {
		Manager manager = new Manager();
		assertNull(manager.getRoot());
		TestComponent c = new TestComponent("root");
		manager.attach(null, c);
		assertTrue(manager.getRoot() == c);
		assertTrue(c.getManager() == manager);
		manager.detach(c);
		assertNull(manager.getRoot());
	}

	@Test
	public void testChildren() {
		Manager manager = new Manager();
		assertNull(manager.getRoot());
		TestComponent c = new TestComponent("root");
		TestComponent c1 = new TestComponent("sub1");
		TestComponent c2 = new TestComponent("sub2");
		manager.attach(null, c);
		manager.attach(c, c1);
		manager.attach(c, c2);
		assertTrue(manager.getChildren(c).size() == 2);
		Iterator<Component> iter = manager.getChildren(c).iterator();
		assertTrue(iter.next() == c1);
		assertTrue(iter.next() == c2);
		assertTrue(c.getManager() == manager);
		assertTrue(c1.getManager() == manager);
		assertTrue(c2.getManager() == manager);
		manager.detach(c2);
		assertTrue(c2.getManager() == null);
		assertTrue(manager.getChildren(c).size() == 1);
		manager.detach(c);
		assertTrue(c.getManager() == null);
		assertTrue(c1.getManager() == null);
	}

}
