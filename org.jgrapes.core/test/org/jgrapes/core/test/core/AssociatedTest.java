/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
public class AssociatedTest {

	public static class TestComponent extends Component {

		public String result;
		
		@Handler(events=Start.class)
		public void onStart(Event<?> evt) {
			evt.setAssociated(this, new String("Hello!"));
		}
		
		@Handler(events=Started.class)
		public void onStarted(Started evt) {
			result = evt.event().associated(this, String.class).get();
		}
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		TestComponent app = new TestComponent();
		Components.manager(app).newSyncEventPipeline().fire(new Start());
		assertTrue(app.result.equals("Hello!"));
	}

}
