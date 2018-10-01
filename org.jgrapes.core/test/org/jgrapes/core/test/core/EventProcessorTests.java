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

package org.jgrapes.core.test.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.internal.CoreUtils;

import org.junit.After;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class EventProcessorTests {

	private Logger fireRestrictionLogger;
	private Level oldLevel;
	
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
		
		@Handler(namedEvents="Override")
		public void onOverride(NamedEvent<Void> event) {
			gotTrigger = true;
			otherPipeline.overrideRestriction()
				.fire(new NamedEvent<Void>("Event"));
		}
		
		@Handler(namedEvents="Event")
		public void onEvent(NamedEvent<Void> event) {
			gotEvent = true;
		}
		
	}

	@Before
	public void setup() {
		fireRestrictionLogger 
			= Logger.getLogger(CoreUtils.class.getPackage().getName()
				+ ".fireRestriction");
		oldLevel = fireRestrictionLogger.getLevel();
		fireRestrictionLogger.setLevel(Level.OFF);
	}

	@After
	public void tearDown() {
		fireRestrictionLogger.setLevel(oldLevel);
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
		
		// With restriction, using illegal, but overriding
		app.newEventPipeline().fire(new NamedEvent<Void>("Override"), app);
		Components.awaitExhaustion();
		assertTrue(app.gotTrigger);
		app.gotTrigger = false;
		assertTrue(app.gotEvent);
		app.gotEvent = false;
		
		// With restriction, using illegal (check that override was reset)
		app.newEventPipeline().fire(new NamedEvent<Void>("Test"), app);
		Components.awaitExhaustion();
		assertTrue(app.gotTrigger);
		app.gotTrigger = false;
		assertFalse(app.gotEvent);
		app.gotEvent = false;
		
		// Without restriction, resetted
		app.otherPipeline.restrictEventSource(null);
		me.fire(new NamedEvent<Void>("Test"), app);
		Components.awaitExhaustion();
		assertTrue(app.gotTrigger);
		app.gotTrigger = false;
		assertTrue(app.gotEvent);
		app.gotEvent = false;
		
	}

}
