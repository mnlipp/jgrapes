/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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

import static org.junit.Assert.*;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.Self;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class ThisTest {

	public static class Component extends AbstractComponent {

		public int count = 0;

		/**
		 * @param componentChannel
		 */
		public Component() {
			super(new NamedChannel("test"));
		}


		@Handler(channels=Self.class)
		public void onStarted(Start event) {
			count += 1;
		}
	}
	
	@Test
	public void testStarted() throws InterruptedException {
		Component app = new Component();
		Components.manager(app).newSyncEventPipeline().fire(new Start(), app);
		assertEquals(1, app.count);
	}

}
