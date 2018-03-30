/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
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
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.annotation.Handler;

import static org.junit.Assert.*;
import org.junit.Test;

public class EventProcessorTests {

	public static class TestApp extends Component {

		EventPipeline otherPipeline;
		boolean gotTrigger = false;
		boolean gotEvent = false;

		public TestApp() {
			otherPipeline = newEventPipeline();
		}
		
		@Handler(namedEvents="Test")
		public void onTest(NamedEvent<Void> event) {
			gotTrigger = true;
			otherPipeline.fire(new NamedEvent<Void>("Event"));
		}
		
		@Handler(namedEvents="Event")
		public void onEvent(NamedEvent<Void> event) {
			gotEvent = true;
		}
		
	}

	@Test
	public void testRestriction() throws InterruptedException {
		// Without restriction
		TestApp app = new TestApp();
		EventPipeline me = app.newEventPipeline();
		Components.start(app);
		me.fire(new NamedEvent<Void>("Test"), app);
		Components.awaitExhaustion();
		assertTrue(app.gotTrigger);
		app.gotTrigger = false;
		assertTrue(app.gotEvent);
		app.gotEvent = false;
		
		// With restriction, using proper
		app.otherPipeline.restrictEventSource(me);
		me.fire(new NamedEvent<Void>("Test"), app);
		Components.awaitExhaustion();
		assertTrue(app.gotTrigger);
		app.gotTrigger = false;
		assertTrue(app.gotEvent);
		app.gotEvent = false;
		
		// With restriction, using illegal
		app.newEventPipeline().fire(new NamedEvent<Void>("Test"), app);
		Components.awaitExhaustion();
		assertTrue(app.gotTrigger);
		app.gotTrigger = false;
		assertFalse(app.gotEvent);
		app.gotEvent = false;
	}

}
