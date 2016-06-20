package org.jgrapes.core.test.core;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.junit.Test;

import static org.junit.Assert.*;

public class StartTest {

	public static class StartCatcher extends AbstractComponent {

		public int startedCount = 0;
		public Channel startedOn;
		
		@Handler
		public void onStart(Start evt) {
			startedCount += 1;
			startedOn = evt.getChannels()[0];
		}
	}
		
	@Test
	public void testStart() 
			throws InterruptedException {
		StartCatcher c1 = new StartCatcher();
		Utils.start(c1);
		assertEquals(1, c1.startedCount);
		assertEquals(Channel.BROADCAST, c1.startedOn);
		StartCatcher c2 = new StartCatcher();
		c1.attach(c2);
		Utils.awaitExhaustion();
		assertEquals(1, c1.startedCount);
		assertEquals(1, c2.startedCount);
		assertEquals(c2, c2.startedOn);
	}

}
