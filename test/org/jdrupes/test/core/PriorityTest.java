package org.jdrupes.test.core;

import org.jdrupes.AbstractComponent;
import org.jdrupes.Component;
import org.jdrupes.Event;
import org.jdrupes.Utils;
import org.jdrupes.annotation.Handler;
import org.jdrupes.events.Start;
import org.jdrupes.events.Started;
import org.junit.Test;

import static org.junit.Assert.*;

public class PriorityTest {

	public static class PrioritisedHandlers extends AbstractComponent {

		public String result = "";
		
		@Handler(events=Start.class)
		public void onStart5(Event evt) {
			result += "o";
		}
		
		@Handler(events=Start.class, priority=2)
		public void onStart3(Event evt) {
			result += "l";
		}
		
		@Handler(events=Start.class, priority=4)
		public void onStart1(Event evt) {
			result += "H";
		}
		
		@Handler(events=Start.class, priority=3)
		public void onStart2(Event evt) {
			result += "e";
		}
		
		@Handler(events=Start.class, priority=1)
		public void onStart4(Event evt) {
			result += "l";
		}
		
	}
	
	@Test
	public void testComplete() {
		PrioritisedHandlers app = new PrioritisedHandlers();
		Utils.start(app);
		assertTrue(app.result.equals("Hello"));
	}

}
