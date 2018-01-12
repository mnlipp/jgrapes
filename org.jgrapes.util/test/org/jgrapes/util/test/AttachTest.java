package org.jgrapes.util.test;


import static org.junit.Assert.*;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.util.PreferencesStore;
import org.jgrapes.util.events.PreferencesInitialized;
import org.junit.Test;

public class AttachTest {

	public static class TestComponent extends Component {
		
		public int initCount = 0;
		
		public TestComponent() {
		}

		public TestComponent(Channel componentChannel) {
			super(componentChannel);
		}

		@Handler
		public void onInitialPreferences(PreferencesInitialized event) {
			initCount += 1;
		}
	}
	
	@Test
	public void testAttachedPrefs() throws InterruptedException {
		
		TestComponent tc1 = new TestComponent();
		TestComponent tc11 = tc1.attach(new TestComponent(tc1));
		tc1.attach(new TestComponent(tc1));
		
		TestComponent tc2 = new TestComponent(tc1);
		tc2.attach(new TestComponent(tc1));
		tc2.attach(new TestComponent(tc1));

		tc1.attach(new PreferencesStore(tc1, AttachTest.class));
		
		Components.start(tc1);
		tc11.attach(tc2);
		Components.awaitExhaustion(1000);
		
		for (ComponentType c: tc1) {
			if (c instanceof TestComponent) {
				assertEquals(1, ((TestComponent)c).initCount);
			}
		}
	}

}
