package org.jdrupes.core.test.core;

import org.jdrupes.core.AbstractComponent;
import org.jdrupes.core.Channel;
import org.jdrupes.core.Event;
import org.jdrupes.core.Utils;
import org.jdrupes.core.annotation.Handler;
import org.jdrupes.core.events.HandlingError;
import org.jdrupes.core.events.Start;
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
