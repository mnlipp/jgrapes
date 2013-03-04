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

public class EventTest {

	public static class CompleteCatcher extends AbstractComponent {
		
		public boolean caughtStart = false;
		public boolean caughtStarted = false;
		
		@Handler(events=Start.class)
		public void onStart(Event evt) {
			caughtStart = true;
		}
		
		@Handler(events=Started.class)
		public void onStarted(Event evt) {
			caughtStarted = true;
		}
	}
	
	@Test
	public void testComplete() {
		CompleteCatcher app = new CompleteCatcher();
		Utils.start(app);
		assertTrue(app.caughtStart);
		assertTrue(app.caughtStarted);
	}

}
