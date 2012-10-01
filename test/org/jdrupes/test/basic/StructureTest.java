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
import org.jdrupes.Utils;
import org.junit.Test;

public class StructureTest {

	private TestComponent subtree1(int offset) {
		TestComponent sub = new TestComponent("node " + offset);
		Utils.manager(sub).addChild
			(new TestComponent("node " + (offset + 1)))
			.addChild(new TestComponent("node " + (offset + 2)));
		return sub;
	}
	
	@Test
	public void testRoot() {
		TestComponent c = new TestComponent("root");
		assertNull(c.getManager());
		Manager manager = Utils.manager(c);
		assertNotNull(manager);
		assertEquals(manager, c.getManager());
		assertEquals(c.getManager().getRoot(), c);
	}

	@Test
	public void testBuild() {
		TestComponent c = new TestComponent("root");
		assertEquals(0, Utils.manager(c).getChildren().size());
		TestComponent c1 = new TestComponent("sub1");
		TestComponent c2 = new TestComponent("sub2");
		c.getManager().addChild(c1).addChild(c2);
		assertEquals(2, c.getManager().getChildren().size());
		Iterator<Component> iter = c.getManager().getChildren().iterator();
		assertTrue(iter.next() == c1);
		assertTrue(iter.next() == c2);
		assertEquals(c1.getManager().getParent(), c);
		assertEquals(c2.getManager().getParent(), c);
		assertEquals(c1.getManager().getRoot(), c);
		assertEquals(c2.getManager().getRoot(), c);
	}
	
	public void testDetach() {
		TestComponent c = new TestComponent("root");
		TestComponent c1 = new TestComponent("sub1");
		TestComponent c2 = new TestComponent("sub2");
		Utils.manager(c).addChild(c1).addChild(c2);
		c1.getManager().detach();
		assertNull(c1.getManager().getParent());
		assertEquals(c1, c1.getManager().getRoot());
		assertEquals(1, c.getManager().getChildren().size());
		c2.getManager().detach();
		assertNull(c2.getManager().getParent());
		assertEquals(c2, c2.getManager().getRoot());
		assertEquals(0, c.getManager().getChildren().size());
	}
	
	@Test
	public void testMove() {
		TestComponent c = new TestComponent("root");
		Utils.manager(c).addChild(subtree1(1)).addChild(subtree1(4));
		Iterator<Component> iter = c.getManager().getChildren().iterator();
		assertEquals("node 1", iter.next().toString());
		assertEquals("node 4", iter.next().toString());
		TestComponent sub1 = (TestComponent)
				c.getManager().getChildren().iterator().next();
		sub1.getManager().detach();
		assertNull(sub1.getManager().getParent());
		assertEquals(2, sub1.getManager().getChildren().size());
		c.getManager().addChild(sub1);
		iter = c.getManager().getChildren().iterator();
		assertEquals("node 4", iter.next().toString());
		assertEquals("node 1", iter.next().toString());
	}

	@Test
	public void testIterator() {
		TestComponent c = subtree1(0);
		Iterator<Component> iter = c.getManager().getChildren().iterator();
		((TestComponent)iter.next()).getManager().addChild(subtree1(3));
		((TestComponent)iter.next()).getManager().addChild(subtree1(6));
		iter = c.getManager().iterator();
		assertEquals("node 0", iter.next().toString());
		assertEquals("node 1", iter.next().toString());
		assertEquals("node 3", iter.next().toString());
		assertEquals("node 4", iter.next().toString());
		assertEquals("node 5", iter.next().toString());
		assertEquals("node 2", iter.next().toString());
		assertEquals("node 6", iter.next().toString());
		assertEquals("node 7", iter.next().toString());
		assertEquals("node 8", iter.next().toString());
	}
}
