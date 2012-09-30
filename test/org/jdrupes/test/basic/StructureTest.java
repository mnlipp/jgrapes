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
import org.jdrupes.ComponentRef;
import org.jdrupes.Manager;
import org.junit.Test;

public class StructureTest {

	private ComponentRef subtree1(Manager manager, int offset) {
		ComponentRef sub = manager.attach
				(manager.detach(new TestComponent("node " + offset)),
				 new TestComponent("node " + (offset + 1)));
		manager.attach(sub, new TestComponent("node " + (offset + 2)));
		return sub;
	}
	
	@Test
	public void testRoot() {
		Manager manager = new Manager();
		assertEquals(manager.getTrees().size(), 0);
		TestComponent c = new TestComponent("root");
		manager.attach(c);
		assertEquals(manager.getTrees().size(), 1);
		assertTrue(manager.getTrees().iterator().next() == c);
		assertTrue(c.getManager() == manager);
		manager.detach(c);
		assertEquals(manager.getTrees().size(), 0);
		assertTrue(c.getManager() == null);
	}

	@Test
	public void testDetachedBuild() {
		Manager manager = new Manager();
		assertEquals(manager.getTrees().size(), 0);
		TestComponent c = new TestComponent("root");
		TestComponent c1 = new TestComponent("sub1");
		TestComponent c2 = new TestComponent("sub2");
		ComponentRef root = manager.attach(manager.detach(c), c1);
		manager.attach(root, c2);
		assertEquals(manager.getTrees().size(), 0);
		manager.attach(root);
		assertEquals(manager.getTrees().iterator().next(), c);
		assertEquals(manager.getParent(c), null);
		assertEquals(manager.getParent(c1), c);
		assertEquals(manager.getParent(c2), c);
		Iterator<Component> iter = manager.getChildren(c).iterator();
		assertTrue(iter.next() == c1);
		assertTrue(iter.next() == c2);
		assertEquals(c.getManager(), manager);
		assertEquals(c1.getManager(), manager);
		assertEquals(c2.getManager(), manager);
	}
	
	public void testDetach() {
		Manager manager = new Manager();
		assertEquals(manager.getTrees().size(), 0);
		TestComponent c = new TestComponent("root");
		TestComponent c1 = new TestComponent("sub1");
		TestComponent c2 = new TestComponent("sub2");
		manager.attach(c);
		manager.attach(c1);
		manager.attach(c2);
		ComponentRef root = manager.detach(c);
		manager.attach(root, c1);
		manager.attach(root, c2);
		assertEquals(manager.getParent(c), null);
		assertEquals(manager.getParent(c1), c);
		assertEquals(manager.getParent(c2), c);
		Iterator<Component> iter = manager.getChildren(c).iterator();
		assertTrue(iter.next() == c1);
		assertTrue(iter.next() == c2);
		manager.detach(c);
		assertEquals(manager.getTrees().size(), 0);
		assertEquals(c.getManager(), null);
		assertEquals(c1.getManager(), null);
		assertEquals(c2.getManager(), null);
	}
	
	@Test
	public void testMove() {
		System.gc();
		Manager manager = new Manager();
		assertEquals(0, manager.getTrees().size());
		TestComponent c = new TestComponent("root");
		manager.attach(c);
		ComponentRef st1 = subtree1(manager, 1);
		ComponentRef st2 = subtree1(manager, 4);
		assertEquals(1, manager.getTrees().size());
		manager.attach(c, st1);
		manager.attach(c, st2);
		manager.detach (st1.getComponent());
		manager.attach(c, st1);
		Iterator<Component> iter = manager.getChildren(c).iterator();
		assertEquals("node 4", iter.next().toString());
		assertEquals("node 1", iter.next().toString());
	}

	@Test
	public void testChildren() {
		Manager manager = new Manager();
		assertEquals(manager.getTrees().size(), 0);
		TestComponent c = new TestComponent("root");
		TestComponent c1 = new TestComponent("sub1");
		TestComponent c2 = new TestComponent("sub2");
		manager.attach(c);
		manager.attach(c, c1);
		manager.attach(c, c2);
		assertEquals(manager.getParent(c), null);
		assertEquals(manager.getParent(c1), c);
		assertEquals(manager.getParent(c2), c);
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
