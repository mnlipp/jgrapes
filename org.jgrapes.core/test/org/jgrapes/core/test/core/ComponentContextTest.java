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

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 */
public class ComponentContextTest {

	public static class TestComponent extends Component {

		public String result;
		
		@Handler(events=Start.class)
		public void onStart(Event<?> evt) {
			evt.setComponentContext(this, new String("Hello!"));
		}
		
		@Handler(events=Started.class)
		public void onStarted(Event<?> evt) {
			result = (String)evt.componentContext(this);
		}
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		TestComponent app = new TestComponent();
		Components.manager(app).newSyncEventPipeline().fire(new Start());
		assertTrue(app.result.equals("Hello!"));
	}

}
