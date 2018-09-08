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

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.annotation.ComponentManager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class ComponentTypeTest {

	public static class ComponentWithDefaulChannel implements ComponentType {

		@ComponentManager
		public Manager manager;
		
		public int count = 0;
		public int count2 = 0;
		
		@Handler(events=Start.class)
		public void onStarted(Event<?> event) throws InterruptedException {
			count += 1;
			// Fire on component's channel
			manager.newEventPipeline().fire(new NamedEvent<>("test")).get();
		}
		
		@Handler(namedEvents="test")
		public void onTest() {
			count2 += 1;
		}
	}

	@Test
	public void testDefaultChannel() throws InterruptedException {
		ComponentWithDefaulChannel app = new ComponentWithDefaulChannel();
		// Register
		Components.manager(app);
		assertEquals(app.manager, app.manager.channel());
		// Fire on dummy
		Components.manager(app).fire(new Start(), 
				new NamedChannel("dummy")).get();
		assertEquals(app.manager, app.manager.channel());
		assertEquals(0, app.count);
		// Fire on component's channel (self)
		Components.manager(app).fire(new Start(), 
				Components.manager(app).channel()).get();
		assertEquals(1, app.count);
		assertEquals(1, app.count2);
	}
	
	@Test
	public void testOverrideDefaultChannel() throws InterruptedException {
		ComponentWithDefaulChannel app = new ComponentWithDefaulChannel();
		Channel channel = new NamedChannel("Override");
		// Register
		Components.manager(app, channel);
		assertEquals(channel, app.manager.channel());
		// Fire
		Components.manager(app, channel).fire(new Start(), channel).get();
		assertEquals(1, app.count);
		assertEquals(1, app.count2);
	}
	
	public static class MyChannel extends ClassChannel {
	}
	
	public static class ComponentWithXtraChannel implements ComponentType {

		@ComponentManager(channel=MyChannel.class)
		private Manager manager;
		
		public int count = 0;
		public int count2 = 0;
		
		@Handler(events=Start.class)
		public void onStarted(Event<?> event) throws InterruptedException {
			count += 1;
			// Fire on component's channel
			manager.newEventPipeline().fire(new NamedEvent<>("test")).get();
		}
		
		@Handler(namedEvents="test", channels=MyChannel.class)
		public void onTest() {
			count2 += 1;
		}
	}
	
	@Test
	public void testXtraChannel() throws InterruptedException {
		ComponentWithXtraChannel app = new ComponentWithXtraChannel();
		// Register
		Components.manager(app);
		assertEquals(new MyChannel(), app.manager.channel());
		// Fire
		Components.manager(app).fire(new Start(), 
				new MyChannel()).get();
		assertEquals(1, app.count);
		assertEquals(1, app.count2);
	}
	
	@Test
	public void testOverrideXtraChannel() throws InterruptedException {
		ComponentWithXtraChannel app = new ComponentWithXtraChannel();
		Channel channel = new NamedChannel("Override");
		// Register
		Components.manager(app, channel);
		assertEquals(channel, app.manager.channel());
		// Fire
		Components.manager(app, channel).fire(new Start(), channel).get();
		assertEquals(channel, app.manager.channel());
		assertEquals(1, app.count);
		assertEquals(0, app.count2);
	}
	
}
