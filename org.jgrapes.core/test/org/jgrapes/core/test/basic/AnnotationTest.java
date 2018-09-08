/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;
import org.junit.Test;

public class AnnotationTest {

	@Test
	public void testAnnotations() {
		Object obj = new TestComponent1();
		Map<String, Annotation> found = new HashMap<String, Annotation>();
		for (Method m: obj.getClass().getMethods()) {
			Annotation anno = m.getAnnotation(Handler.class);
			if (anno != null) {
				found.put(m.getName(), anno);
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

	private class BadMethod extends Component {

		@Handler
		public void onEvent(int param) {
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadMethod() {
		new BadMethod();
	}
}
