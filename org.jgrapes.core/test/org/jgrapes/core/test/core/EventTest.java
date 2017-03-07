package org.jgrapes.core.test.core;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;

import static org.junit.Assert.*;
import org.junit.Test;

public class EventTest {

	public static class CompleteCatcher extends Component {
		
		public boolean caughtStart = false;
		public boolean caughtStarted = false;
		
		@Handler(events=Start.class)
		public void onStart(Event<?> evt) {
			caughtStart = true;
			try {
				evt.setChannels(new Channel[] { Channel.BROADCAST });
				fail();
			} catch (IllegalStateException e) {
				// Expected
			}
		}
		
		@Handler(events=Started.class)
		public void onStarted(Event<?> evt) {
			caughtStarted = true;
		}
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		CompleteCatcher app = new CompleteCatcher();
		Components.manager(app).newSyncEventPipeline().fire(new Start());
		assertTrue(app.caughtStart);
		assertTrue(app.caughtStarted);
	}

}
