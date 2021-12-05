package org.jgrapes.core.test.core;

import org.jgrapes.core.Components.PoolingIndex;
import static org.junit.Assert.*;
import org.junit.Test;

public class PoolingIndexTest {

    @Test
    public void test() {
        PoolingIndex<Integer, Object> pi = new PoolingIndex<>();
        Object entry1 = new Object();
        Object entry2 = new Object();
        pi.add(42, entry1);
        pi.add(42, entry2);
        assertNull(pi.poll(0));
        Object retrieved = pi.poll(42);
        assertTrue(retrieved == entry1 || retrieved == entry2);
        retrieved = pi.poll(42);
        assertTrue(retrieved == entry1 || retrieved == entry2);
        assertEquals(0, pi.keysSize());
        {
            pi.add(42, new Object());
            assertTrue(pi.containsKey(42));
        }
        System.gc();
        assertNull(pi.poll(42));
    }

}
