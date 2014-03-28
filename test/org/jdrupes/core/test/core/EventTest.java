package org.jdrupes.core.test.core;

import org.jdrupes.core.AbstractComponent;
import org.jdrupes.core.Channel;
import org.jdrupes.core.Event;
import org.jdrupes.core.Utils;
import org.jdrupes.core.annotation.Handler;
import org.jdrupes.core.events.Start;
import org.jdrupes.core.events.Started;
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
