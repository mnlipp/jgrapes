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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jdrupes.annotation.Handler;
import org.jdrupes.events.Start;
import org.junit.Test;

public class AnnotationTest {

	@Test
	public void testAnnotations() {
		Object o = new TestComponent1();
		Map<String, Annotation> found = new HashMap<String, Annotation>();
		for (Method m: o.getClass().getMethods()) {
			Annotation a = m.getAnnotation(Handler.class);
			if (a != null) {
				found.put(m.getName(), a);
			}
		}
		assertEquals(found.size(), 3);
		assertTrue(found.containsKey("handler1"));
		assertTrue(found.containsKey("handler2"));
		assertTrue(found.containsKey("handler3"));
		assertTrue(((Handler)found.get("handler1"))
				   .events()[0].equals(Start.class));
		assertTrue(((Handler)found.get("handler2"))
				   .namedChannels()[0].equals("test"));
		assertTrue(((Handler)found.get("handler3"))
				   .namedChannels()[0].equals("test"));
		assertTrue(((Handler)found.get("handler3"))
				   .namedChannels()[1].equals("other"));
	}

}
