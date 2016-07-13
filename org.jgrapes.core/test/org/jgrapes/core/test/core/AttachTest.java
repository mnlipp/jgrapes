package org.jgrapes.core.test.core;

import org.jgrapes.core.Component;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentNode;
import org.jgrapes.core.Event;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Attached;
import org.jgrapes.core.events.Detached;
import org.junit.Test;

import static org.junit.Assert.*;

public class AttachTest {

	public static class AttachCatcher extends Component {

		public AttachCatcher() {
			super(Channel.BROADCAST);
		}
		
		public ComponentNode attachRoot = null;
		public ComponentNode attachParent = null;
		public ComponentNode attachChild = null;
		public ComponentNode detachParent = null;
		public ComponentNode detachChild = null;
		
		@Handler
		public void onAttached(Attached evt) {
			if (evt.getParent() == null) {
				attachRoot = evt.getNode();
			}
			attachParent = evt.getParent();
			attachChild = evt.getNode();
		}
		
		@Handler
		public void onDetached(Detached evt) {
			detachParent = evt.getParent();
			detachChild = evt.getNode();
		}
	}
	
	@Test
	public void testPostStart() 
			throws InterruptedException {
		AttachCatcher c1 = new AttachCatcher();
		AttachCatcher c2 = new AttachCatcher();
		c1.attach(c2);
		Components.start(c1);
		assertEquals(c1, c1.attachRoot);
		assertEquals(c1, c2.attachRoot);
		assertEquals(c1, c1.attachParent);
		assertEquals(c2, c1.attachChild);
		assertEquals(c1, c2.attachParent);
		assertEquals(c2, c2.attachChild);
		c2.detach();
		c1.fire(new Event<Void>()).get();
		c2.fire(new Event<Void>()).get();
		assertEquals(c1, c1.detachParent);
		assertEquals(c2, c1.detachChild);
		assertEquals(c1, c2.detachParent);
		assertEquals(c2, c2.detachChild);
	}

}
