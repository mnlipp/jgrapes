package org.jgrapes.core.test.core;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;
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
	public void testComplete() throws InterruptedException {
		CompleteCatcher app = new CompleteCatcher();
		Utils.manager(app).newSyncEventPipeline().add(new Start());
		assertTrue(app.caughtStart);
		assertTrue(app.caughtStarted);
	}

}
