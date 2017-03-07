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
