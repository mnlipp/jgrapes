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
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;
import org.junit.Test;

public class PriorityTest {

	public static class PrioritisedHandlers extends Component {

		public String result = "";
		
		@Handler(events=Start.class)
		public void onStart5(Event<?> evt) {
			result += "o";
		}
		
		@Handler(events=Start.class, priority=2)
		public void onStart3(Event<?> evt) {
			result += "l";
		}
		
		@Handler(events=Start.class, priority=4)
		public void onStart1(Event<?> evt) {
			result += "H";
		}
		
		@Handler(events=Start.class, priority=3)
		public void onStart2(Event<?> evt) {
			result += "e";
		}
		
		@Handler(events=Start.class, priority=1)
		public void onStart4(Event<?> evt) {
			result += "l";
		}
		
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		PrioritisedHandlers app = new PrioritisedHandlers();
		Components.start(app);
		assertTrue(app.result.equals("Hello"));
	}

}
