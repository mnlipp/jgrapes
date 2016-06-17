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
package org.jgrapes.core.test.basic;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Component;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Utils;
import org.junit.Test;

public class StructureTest {

	private TestComponent1 subtree1(int offset) {
		TestComponent1 sub = new TestComponent1("node " + offset);
		Utils.manager(sub).attach
			(new TestComponent1("node " + (offset + 1)))
			.attach(new TestComponent1("node " + (offset + 2)));
		return sub;
	}
	
	@Test
	public void testRoot() {
		TestComponent1 c = new TestComponent1("root");
		assertNull(c.getManager());
		// Set manager
		Manager manager = Utils.manager(c);
		assertNotNull(manager);
		assertEquals(manager, c.getManager());
		// Retrieve existing manager
		assertEquals(manager, Utils.manager(c));
		assertEquals(c.getManager().getRoot(), c);
		assertEquals("Test", manager.getChannel().getMatchKey());
	}

	@Test
	public void testBuild() {
		TestComponent1 c = new TestComponent1("root");
		assertEquals(0, Utils.manager(c).getChildren().size());
		TestComponent1 c1 = new TestComponent1("sub1");
		TestComponent1 c2 = new TestComponent1("sub2");
		c.getManager().attach(c1).attach(c2);
		assertEquals(2, c.getManager().getChildren().size());
		Iterator<Component> iter = c.getManager().getChildren().iterator();
		assertSame(iter.next(), c1);
		assertSame(iter.next(), c2);
		assertEquals(c1.getManager().getParent(), c);
		assertEquals(c2.getManager().getParent(), c);
		assertEquals(c1.getManager().getRoot(), c);
		assertEquals(c2.getManager().getRoot(), c);
	}
	
	@Test
	public void testDetach() throws InterruptedException, ExecutionException {
		TestComponent1 c = new TestComponent1("root");
		Utils.start(c);
		TestComponent1 c1 = new TestComponent1("sub1");
		TestComponent1 c2 = new TestComponent1("sub2");
		Utils.manager(c).attach(c1).attach(c2);
		c1.getManager().detach();
		assertNull(c1.getManager().getParent());
		assertEquals(c1, c1.getManager().getRoot());
		assertEquals(1, c.getManager().getChildren().size());
		c1.getManager().detach(); // detach again, nothing may change
		assertNull(c1.getManager().getParent());
		assertEquals(c1, c1.getManager().getRoot());
		assertEquals(1, c.getManager().getChildren().size());
		c2.getManager().detach();
		assertNull(c2.getManager().getParent());
		assertEquals(c2, c2.getManager().getRoot());
		assertEquals(0, c.getManager().getChildren().size());
	}
	
	@Test
	public void testIterator() {
		TestComponent1 c = subtree1(0);
		Iterator<Component> iter = c.getManager().getChildren().iterator();
		((TestComponent1)iter.next()).getManager().attach(subtree1(3));
		((TestComponent1)iter.next()).getManager().attach(subtree1(6));
		iter = c.getManager().iterator();
		assertTrue(iter.hasNext());
		try {
			iter.remove();
			fail();
		} catch (UnsupportedOperationException e) {
		}
		assertEquals("node 0", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 1", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 3", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 4", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 5", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 2", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 6", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 7", iter.next().toString());
		assertTrue(iter.hasNext());
		assertEquals("node 8", iter.next().toString());
		assertFalse(iter.hasNext());
		try {
			iter.next();
			fail();
		} catch (NoSuchElementException e) {
		}
	}
	
	@Test
	public void testDerived() {
		TestComponent2 c = new TestComponent2("root");
		TestComponent2 c1 = new TestComponent2("sub1");
		TestComponent2 c2 = new TestComponent2("sub2");
		c.attach(c1).attach(c2);
		Iterator<Component> iter = c.getChildren().iterator();
		assertSame(iter.next(), c1);
		assertSame(iter.next(), c2);
	}
	
	public static class TCD extends TestComponent1 {		
	}
	
	@Test
	public void testInheritedManager () {
		TCD c = new TCD();
		Manager mgr = Utils.manager(c);
		assertNotNull(mgr);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testIllegalComponent() {
		Utils.manager(new IllegalComponent());
	}
}
