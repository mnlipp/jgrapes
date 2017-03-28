package org.jgrapes.core.test.core;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;
import org.junit.Test;

public class ErrorTest {

	public static class BuggyComponent extends Component {
		
		public boolean caughtError = false;
		
		@Handler(events=Start.class)
		public void onStart(Event<?> evt) {
			throw new IllegalStateException();
		}
		
		@Handler(events=HandlingError.class, channels=Channel.class)
		public void onError(HandlingError evt) {
			if (evt.throwable().getClass() == IllegalStateException.class) {
				caughtError = true;
			}
		}
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		BuggyComponent app = new BuggyComponent();
		Components.start(app);
		assertTrue(app.caughtError);
	}

}
