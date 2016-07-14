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

import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.AttachedComponent;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.ComponentManager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.junit.Test;

/**
 * @author mnl
 *
 */
public class ComponentClassChannelTest {

	public static class MyChannel extends ClassChannel {
	}
	
	public static class ComponentWithClassChannel implements AttachedComponent {

		@ComponentManager(channel=MyChannel.class)
		private Manager manager;
		
		public int count = 0;
		
		@Handler(events=Start.class)
		public void onStarted(Event<?> event) {
			count += 1;
		}
	}

	@Test
	public void testClassChannel() throws InterruptedException {
		ComponentWithClassChannel app = new ComponentWithClassChannel();
		Components.manager(app).fire(new Start(), new MyChannel()).get();
		assertEquals(1, app.count);
	}
	
}
