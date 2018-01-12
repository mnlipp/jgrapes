package org.jgrapes.core.test.core;


import static org.junit.Assert.*;

import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.junit.Test;

public class StartTest {

	public static class TestComponent extends Component {
		
		public int startCount = 0;
		
		@Handler
		public void onStart(Start event) {
			startCount += 1;
		}
	}
	
	@Test
	public void testAttachedStart() throws InterruptedException {
		
		TestComponent tc1 = new TestComponent();
		TestComponent tc11 = tc1.attach(new TestComponent());
		tc1.attach(new TestComponent());
		
		TestComponent tc2 = new TestComponent();
		tc2.attach(new TestComponent());
		tc2.attach(new TestComponent());
		
		Components.start(tc1);
		tc11.attach(tc2);
		Components.awaitExhaustion(1000);
		
		for (ComponentType c: tc1) {
			assertEquals(1, ((TestComponent)c).startCount);
		}
	}

}
