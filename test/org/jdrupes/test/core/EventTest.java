package org.jdrupes.test.core;

import org.jdrupes.AbstractComponent;
import org.jdrupes.Channel;
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
			try {
				evt.setChannels(new Channel[] { Channel.BROADCAST });
				fail();
			} catch (IllegalStateException e) {
			}
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
