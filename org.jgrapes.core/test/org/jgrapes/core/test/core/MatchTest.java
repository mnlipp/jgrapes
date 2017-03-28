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
 * @author mnl
 *
 */
public class MatchTest {

	public class EventCounter extends Component {

		public int all = 0;
		public int startedGlobal = 0;
		public int startedTest1 = 0;
		public int named1Global = 0;
		public int named1Test1 = 0;
		public int startedComponent = 0;

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
			startedComponent += 1;
		}
		
		@Handler(events=Event.class, channels=Channel.class)
		public void onAll(Event<?> event) {
			all += 1;
		}

		@Handler(dynamic=true)
		public void onStart(Start event) {
			startedGlobal += 1;
		}

		@Handler(events=Start.class, namedChannels="test1")
		public void onStartTest1(Start event) {
			startedTest1 += 1;
		}

		@Handler(namedEvents="named1")
		public void onNamed1(Event<?> event) {
			named1Global += 1;
		}
		
		@Handler(namedEvents="named1", namedChannels="test1")
		public void onNamed1Test1(Event<?> event) {
			named1Test1 += 1;
		}
	}

	@Test
	public void testEventCounter() throws InterruptedException {
		EventCounter app = new EventCounter();
		EventPipeline pipeline = Components.manager(app).newSyncEventPipeline();
		pipeline.fire(new Start());
		Components.awaitExhaustion();
		assertEquals(1, app.startedGlobal);
		assertEquals(1, app.startedTest1);
		assertEquals(0, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(2, app.all); // Start and Started
		pipeline.fire(new Start(), new NamedChannel("test1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(0, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(4, app.all);	// Start and Started
		pipeline.fire(new NamedEvent<Void>("named1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(1, app.named1Global);
		assertEquals(1, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(5, app.all);	// NamedEvent
		pipeline.fire(new NamedEvent<Void>("named1"), new NamedChannel("test1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(2, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(6, app.all);	// NamedEvent
		pipeline.fire(new Start(), app);
		assertEquals(3, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(2, app.named1Test1);
		assertEquals(2, app.startedComponent);
		assertEquals(8, app.all);	// Start and Started
		pipeline.fire(new Start(), app);
		assertEquals(4, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(2, app.named1Test1);
		assertEquals(3, app.startedComponent);
		assertEquals(10, app.all);	// Start and Started
	}
}
