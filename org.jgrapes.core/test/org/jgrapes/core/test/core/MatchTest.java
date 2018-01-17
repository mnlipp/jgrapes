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

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class MatchTest {

	public class EventCounter extends Component {

		public int anyDirectedAtAnyChannel = 0;
		public int startDirectedAtComponentChannel = 0;
		public int startDirectedAtTest1 = 0;
		public int named1DirectedAtComponentChannel = 0;
		public int named1DirectedAtTest1 = 0;
		public int startDirectedAtComponent = 0;

		public void reset() {
			anyDirectedAtAnyChannel = 0;
			startDirectedAtComponentChannel = 0;
			startDirectedAtTest1 = 0;
			named1DirectedAtComponentChannel = 0;
			named1DirectedAtTest1 = 0;
			startDirectedAtComponent = 0;
		}
		
		/**
		 * 
		 */
		public EventCounter() {
			super(Channel.BROADCAST);
			Handler.Evaluator.add(
					this, "onStartedComponent", Start.class, this, 0);
			Handler.Evaluator.add(this, "onStart", channel());
		}

		@Handler(dynamic=true)
		public void onStartedComponent(Start event) {
			startDirectedAtComponent += 1;
		}
		
		@Handler(dynamic=true)
		public void onStart(Start event) {
			startDirectedAtComponentChannel += 1;
		}

		@Handler(events=Event.class, channels=Channel.class)
		public void onAll(Event<?> event) {
			anyDirectedAtAnyChannel += 1;
		}

		@Handler(events=Start.class, namedChannels="test1")
		public void onStartTest1(Start event) {
			startDirectedAtTest1 += 1;
		}

		@Handler(namedEvents="named1")
		public void onNamed1(Event<?> event) {
			named1DirectedAtComponentChannel += 1;
		}
		
		@Handler(namedEvents="named1", namedChannels="test1")
		public void onNamed1Test1(Event<?> event) {
			named1DirectedAtTest1 += 1;
		}
	}

	@Test
	public void testEventCounter() throws InterruptedException {
		EventCounter app = new EventCounter();
		EventPipeline pipeline = Components.manager(app).newSyncEventPipeline();
		pipeline.fire(new Start());
		Components.awaitExhaustion();
		assertEquals(1, app.startDirectedAtComponent);
		assertEquals(1, app.startDirectedAtComponentChannel);
		assertEquals(1, app.startDirectedAtTest1);
		assertEquals(0, app.named1DirectedAtComponentChannel);
		assertEquals(0, app.named1DirectedAtTest1);
		assertEquals(2, app.anyDirectedAtAnyChannel); // Start and Started
		app.reset();
		pipeline.fire(new Start(), new NamedChannel("test1"));
		assertEquals(0, app.startDirectedAtComponent);
		assertEquals(1, app.startDirectedAtComponentChannel);
		assertEquals(1, app.startDirectedAtTest1);
		assertEquals(0, app.named1DirectedAtComponentChannel);
		assertEquals(0, app.named1DirectedAtTest1);
		assertEquals(2, app.anyDirectedAtAnyChannel);	// Start and Started
		app.reset();
		pipeline.fire(new NamedEvent<Void>("named1"));
		assertEquals(0, app.startDirectedAtComponent);
		assertEquals(0, app.startDirectedAtComponentChannel);
		assertEquals(0, app.startDirectedAtTest1);
		assertEquals(1, app.named1DirectedAtComponentChannel);
		assertEquals(1, app.named1DirectedAtTest1);
		assertEquals(1, app.anyDirectedAtAnyChannel);	// NamedEvent
		app.reset();
		pipeline.fire(new NamedEvent<Void>("named1"), new NamedChannel("test1"));
		assertEquals(0, app.startDirectedAtComponent);
		assertEquals(0, app.startDirectedAtComponentChannel);
		assertEquals(0, app.startDirectedAtTest1);
		assertEquals(1, app.named1DirectedAtComponentChannel);
		assertEquals(1, app.named1DirectedAtTest1);
		assertEquals(1, app.anyDirectedAtAnyChannel);	// NamedEvent
		app.reset();
		pipeline.fire(new Start(), app);
		assertEquals(1, app.startDirectedAtComponent);
		assertEquals(1, app.startDirectedAtComponentChannel);
		assertEquals(1, app.startDirectedAtTest1);
		assertEquals(0, app.named1DirectedAtComponentChannel);
		assertEquals(0, app.named1DirectedAtTest1);
		assertEquals(2, app.anyDirectedAtAnyChannel);	// Start and Started
	}
}
