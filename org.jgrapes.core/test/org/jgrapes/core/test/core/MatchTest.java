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

import static org.junit.Assert.*;

import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.Utils;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.test.core.components.ComponentWOChannel;
import org.jgrapes.core.test.core.components.ComponentWithBroadcastChannel;
import org.jgrapes.core.test.core.components.ComponentWithClassChannel;
import org.jgrapes.core.test.core.components.EventCounter;
import org.junit.Test;

/**
 * @author mnl
 *
 */
public class MatchTest {

	@Test
	public void testEventCounter() throws InterruptedException {
		EventCounter app = new EventCounter();
		EventPipeline pipeline = Utils.manager(app).newSyncEventPipeline();
		pipeline.add(new Start());
		assertEquals(1, app.startedGlobal);
		assertEquals(1, app.startedTest1);
		assertEquals(0, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(2, app.all); // Start and Started
		pipeline.add(new Start(), new NamedChannel("test1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(0, app.named1Global);
		assertEquals(0, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(4, app.all);	// Start and Started
		pipeline.add(new NamedEvent<Void>("named1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(1, app.named1Global);
		assertEquals(1, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(5, app.all);	// NamedEvent
		pipeline.add(new NamedEvent<Void>("named1"), new NamedChannel("test1"));
		assertEquals(2, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(2, app.named1Test1);
		assertEquals(1, app.startedComponent);
		assertEquals(6, app.all);	// NamedEvent
		pipeline.add(new Start(), app);
		assertEquals(3, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(2, app.named1Test1);
		assertEquals(2, app.startedComponent);
		assertEquals(8, app.all);	// Start and Started
		pipeline.add(new Start(), app);
		assertEquals(4, app.startedGlobal);
		assertEquals(2, app.startedTest1);
		assertEquals(2, app.named1Global);
		assertEquals(2, app.named1Test1);
		assertEquals(3, app.startedComponent);
		assertEquals(10, app.all);	// Start and Started
	}

	@Test
	public void testWOChannel() throws InterruptedException {
		ComponentWOChannel app = new ComponentWOChannel();
		Utils.start(app);
		assertEquals(1, app.count);
	}
	
	@Test
	public void testClassChannel() {
		ComponentWithClassChannel app = new ComponentWithClassChannel();
		Utils.manager(app).fire(new Start(), 
				new ComponentWithClassChannel.MyChannel());
	}
	
	@Test
	public void testBroadcastChannel() {
		ComponentWithBroadcastChannel app = new ComponentWithBroadcastChannel();
		Utils.manager(app).fire(new Start(), 
				new ComponentWithClassChannel.MyChannel());
	}
}
