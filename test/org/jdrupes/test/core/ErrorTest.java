package org.jdrupes.test.core;

import org.jdrupes.AbstractComponent;
import org.jdrupes.Channel;
import org.jdrupes.Event;
import org.jdrupes.Utils;
import org.jdrupes.annotation.Handler;
import org.jdrupes.events.HandlingError;
import org.jdrupes.events.Start;
import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorTest {

	public static class BuggyComponent extends AbstractComponent {
		
		public boolean caughtError = false;
		
		@Handler(events=Start.class)
		public void onStart(Event evt) {
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
	public void testComplete() {
		BuggyComponent app = new BuggyComponent();
		Utils.start(app);
		assertTrue(app.caughtError);
	}

}
