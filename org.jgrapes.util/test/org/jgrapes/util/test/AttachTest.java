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

package org.jgrapes.util.test;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.PreferencesStore;
import org.jgrapes.util.events.InitialPreferences;

import static org.junit.Assert.*;

import org.junit.Test;

public class AttachTest {

	public static class TestComponent extends Component {
		
		public int initCount = 0;
		
		public TestComponent() {
		}

		public TestComponent(Channel componentChannel) {
			super(componentChannel);
		}

		@Handler
		public void onInitialPreferences(InitialPreferences event) {
			initCount += 1;
		}
	}
	
	@Test
	public void testAttachedPrefs() throws InterruptedException {
		
		TestComponent tc1 = new TestComponent();
		final TestComponent tc11 = tc1.attach(new TestComponent(tc1));
		tc1.attach(new TestComponent(tc1));
		
		TestComponent tc2 = new TestComponent(tc1);
		tc2.attach(new TestComponent(tc1));
		tc2.attach(new TestComponent(tc1));

		tc1.attach(new PreferencesStore(tc1, AttachTest.class));
		
		Components.start(tc1);
		tc11.attach(tc2);
		Components.awaitExhaustion(1000);
		
		for (ComponentType c: tc1) {
			if (c instanceof TestComponent) {
				assertEquals(1, ((TestComponent)c).initCount);
			}
		}
	}

}
