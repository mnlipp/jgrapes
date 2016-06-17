package org.jgrapes.core.test.core;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.events.Start;
import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorTest {

	public static class BuggyComponent extends AbstractComponent {
		
		public boolean caughtError = false;
		
		@Handler(events=Start.class)
		public void onStart(Event<?> evt) {
			throw new IllegalStateException();
		}
		
		@Handler(events=HandlingError.class, channels=Channel.class)
		public void onError(HandlingError evt) {
			if (evt.getThrowable().getClass() == IllegalStateException.class) {
				caughtError = true;
			}
		}
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		BuggyComponent app = new BuggyComponent();
		Utils.start(app);
		assertTrue(app.caughtError);
	}

}
