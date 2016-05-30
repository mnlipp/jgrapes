package org.jgrapes.core.test.core;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Component;
import org.jgrapes.core.Event;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Attached;
import org.jgrapes.core.events.Detached;
import org.junit.Test;

import static org.junit.Assert.*;

public class AttachTest {

	public static class AttachCatcher extends AbstractComponent {

		public Component attachRoot = null;
		public Component attachParent = null;
		public Component attachChild = null;
		public Component detachParent = null;
		public Component detachChild = null;
		
		@Handler(events=Attached.class)
		public void onAttached(Attached evt) {
			if (evt.getParent() == null) {
				attachRoot = evt.getNode();
			}
			attachParent = evt.getParent();
			attachChild = evt.getNode();
		}
		
		@Handler(events=Detached.class)
		public void onDetached(Detached evt) {
			detachParent = evt.getParent();
			detachChild = evt.getNode();
		}
	}
	
	@Test
	public void testPostStart() throws InterruptedException {
		AttachCatcher c1 = new AttachCatcher();
		AttachCatcher c2 = new AttachCatcher();
		c1.attach(c2);
		Utils.start(c1);
		assertEquals(c1, c1.attachRoot);
		assertEquals(c1, c2.attachRoot);
		assertEquals(c1, c1.attachParent);
		assertEquals(c2, c1.attachChild);
		assertEquals(c1, c2.attachParent);
		assertEquals(c2, c2.attachChild);
		c2.detach();
		Utils.fireAndAwait(c1, new Event());
		Utils.fireAndAwait(c2, new Event());
		assertEquals(c1, c1.detachParent);
		assertEquals(c2, c1.detachChild);
		assertEquals(c1, c2.detachParent);
		assertEquals(c2, c2.detachChild);
	}

}
