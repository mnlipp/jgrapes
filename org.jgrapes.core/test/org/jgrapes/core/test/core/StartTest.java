/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.test.core;

import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;

import org.junit.Test;

public class StartTest {

	public static class TestComponent extends Component {
		
		public int startCount = 0;
		
		@Handler
		public void onStart(Start event) {
			startCount += 1;
		}
	}
	
	@Test
	public void testAttachedStart() throws InterruptedException {
		
		final TestComponent tc1 = new TestComponent();
		final TestComponent tc11 = tc1.attach(new TestComponent());
		tc1.attach(new TestComponent());
		
		final TestComponent tc2 = new TestComponent();
		tc2.attach(new TestComponent());
		tc2.attach(new TestComponent());
		
		Components.start(tc1);
		tc11.attach(tc2);
		Components.awaitExhaustion(1000);
		
		for (ComponentType c: tc1) {
			assertEquals(1, ((TestComponent)c).startCount);
		}
	}

}
