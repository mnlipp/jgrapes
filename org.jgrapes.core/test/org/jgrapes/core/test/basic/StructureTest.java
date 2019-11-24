/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.test.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Manager;

import static org.junit.Assert.*;
import org.junit.Test;

public class StructureTest {

    private TestComponent1 subtree1(int offset) {
        TestComponent1 sub = new TestComponent1("node " + offset);
        Components.manager(sub).attach(
            new TestComponent1("node " + (offset + 1)));
        Components.manager(sub)
            .attach(new TestComponent1("node " + (offset + 2)));
        return sub;
    }

    @Test
    public void testRoot() {
        TestComponent1 comp = new TestComponent1("root");
        assertNull(comp.getManager());
        // Set manager
        Manager manager = Components.manager(comp);
        assertNotNull(manager);
        assertEquals(manager, comp.getManager());
        // Retrieve existing manager
        assertEquals(manager, Components.manager(comp));
        assertEquals(comp.getManager().root(), comp);
        assertEquals("Test", manager.channel().defaultCriterion());
    }

    @Test
    public void testBuild() {
        TestComponent1 comp = new TestComponent1("root");
        assertEquals(0, Components.manager(comp).children().size());
        TestComponent1 comp1
            = comp.getManager().attach(new TestComponent1("sub1"));
        TestComponent1 comp2
            = comp.getManager().attach(new TestComponent1("sub2"));
        assertEquals(2, comp.getManager().children().size());
        Iterator<ComponentType> iter = comp.getManager().children().iterator();
        assertSame(iter.next(), comp1);
        assertSame(iter.next(), comp2);
        assertEquals(comp1.getManager().parent(), comp);
        assertEquals(comp2.getManager().parent(), comp);
        assertEquals(comp1.getManager().root(), comp);
        assertEquals(comp2.getManager().root(), comp);
    }

    @Test
    public void testDetach() throws InterruptedException, ExecutionException {
        TestComponent1 comp = new TestComponent1("root");
        Components.start(comp);
        final TestComponent1 comp1
            = Components.manager(comp).attach(new TestComponent1("sub1"));
        final TestComponent1 comp2
            = Components.manager(comp).attach(new TestComponent1("sub2"));
        comp1.getManager().detach();
        assertNull(comp1.getManager().parent());
        assertEquals(comp1, comp1.getManager().root());
        assertEquals(1, comp.getManager().children().size());
        comp1.getManager().detach(); // detach again, nothing may change
        assertNull(comp1.getManager().parent());
        assertEquals(comp1, comp1.getManager().root());
        assertEquals(1, comp.getManager().children().size());
        comp2.getManager().detach();
        assertNull(comp2.getManager().parent());
        assertEquals(comp2, comp2.getManager().root());
        assertEquals(0, comp.getManager().children().size());
    }

    @Test
    public void testIterator() {
        TestComponent1 comp = subtree1(0);
        Iterator<ComponentType> iter = comp.getManager().children().iterator();
        ((TestComponent1) iter.next()).getManager().attach(subtree1(3));
        ((TestComponent1) iter.next()).getManager().attach(subtree1(6));
        iter = comp.getManager().iterator();
        assertTrue(iter.hasNext());
        try {
            iter.remove();
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
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
            // Excepted
        }
    }

    @Test
    public void testDerived() {
        TestComponent2 comp = new TestComponent2("root");
        TestComponent2 comp1 = comp.attach(new TestComponent2("sub1"));
        TestComponent2 comp2 = comp.attach(new TestComponent2("sub2"));
        Iterator<ComponentType> iter = comp.children().iterator();
        assertSame(iter.next(), comp1);
        assertSame(iter.next(), comp2);
    }

    public static class Tcd extends TestComponent1 {
    }

    @Test
    public void testInheritedManager() {
        Tcd comp = new Tcd();
        Manager mgr = Components.manager(comp);
        assertNotNull(mgr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalComponent() {
        Components.manager(new IllegalComponent());
    }

    public static class AnonymousComponent extends Component {
    }

    @Test
    public void testPaths() {
        final TestComponent2 comp = new TestComponent2("root");
        final TestComponent2 comp1 = comp.attach(new TestComponent2("sub1"));
        final TestComponent2 comp2 = comp.attach(new TestComponent2("sub2"));
        final TestComponent1 comp3 = comp.attach(new TestComponent1("sub3"));
        final AnonymousComponent comp4 = comp1.attach(new AnonymousComponent());
        Iterator<ComponentType> iter = comp.children().iterator();
        assertSame(iter.next(), comp1);
        assertSame(iter.next(), comp2);
        assertEquals("/root/sub1", comp1.componentPath());
        assertEquals("/root/sub2", comp2.componentPath());
        assertEquals("/root/ComponentProxy",
            Components.manager(comp3).componentPath());
        assertEquals("/root/sub1/AnonymousComponent", comp4.componentPath());
    }

}
