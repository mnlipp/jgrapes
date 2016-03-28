package org.jgrapes.core.test.core;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Component;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Attached;
import org.jgrapes.core.events.Detached;
import org.junit.Test;

import static org.junit.Assert.*;

public class RegisterTest {

	public static class RegisterCatcher extends AbstractComponent {
		
		public Component attachParent = null;
		public Component attachChild = null;
		public Component detachParent = null;
		public Component detachChild = null;
		
		@Handler(events=Attached.class)
		public void onAttached(Attached evt) {
			attachParent = evt.getParent();
			attachChild = evt.getChild();
		}
		
		@Handler(events=Detached.class)
		public void onDetached(Detached evt) {
			detachParent = evt.getParent();
			detachChild = evt.getChild();
		}
	}
	
	@Test
	public void testPostStart() {
		RegisterCatcher c1 = new RegisterCatcher();
		RegisterCatcher c2 = new RegisterCatcher();
		c1.attach(c2);
		Utils.start(c1);
		assertEquals(c1, c1.attachParent);
		assertEquals(c2, c1.attachChild);
		assertEquals(c1, c2.attachParent);
		assertEquals(c2, c2.attachChild);
		c2.detach();
		assertEquals(c1, c1.detachParent);
		assertEquals(c2, c1.detachChild);
		assertEquals(c1, c2.detachParent);
		assertEquals(c2, c2.detachChild);
	}

}
