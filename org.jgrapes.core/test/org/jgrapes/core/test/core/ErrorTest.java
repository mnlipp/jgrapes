package org.jgrapes.core.test.core;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.events.Start;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

public class ErrorTest {

	public static class BuggyComponent extends Component {
		
		@Handler(events=Start.class)
		public void onStart(Event<?> evt) {
			throw new IllegalStateException();
		}
	}
	
	@Test
	public void testDefaultHandler() throws InterruptedException {
		PrintStream oldErr = System.err;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			System.setErr(new PrintStream(bos));
			BuggyComponent app = new BuggyComponent();
			Components.start(app);
			System.err.flush();
			String error = new String(bos.toByteArray());
			assertTrue(error.contains("IllegalStateException"));
		} finally {
			System.setErr(oldErr);
		}
	}
	
	public static class BuggyComponentWithHandler extends Component {
		
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
		BuggyComponentWithHandler app = new BuggyComponentWithHandler();
		Components.start(app);
		assertTrue(app.caughtError);
	}

}
