package org.jgrapes.core.test.core;

import static org.junit.Assert.*;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.junit.Test;

public class CompletedTest {

	private static boolean handled1 = false;
	
	public static class TestEvent1 extends Event<Integer> {

		/* (non-Javadoc)
		 * @see org.jgrapes.core.internal.EventBase#handled()
		 */
		@Override
		protected void handled() {
			synchronized (this) {
				handled1 = true;
				notifyAll();
			}
		}
		
	}
	
	public static class TestEvent2 extends Event<Integer> {
	}
	
	public static class TestClass extends Component {
		
		public int counter = 100;
		public boolean testDone = false;
		
		@Handler
		public void onTest1(TestEvent1 evt) {
			newEventPipeline().fire(new TestEvent2());
		}

		@Handler
		public void onTest2(TestEvent2 evt) throws InterruptedException {
			synchronized (this) {
				while (counter > 0) {
					wait();
				}				
			}
			testDone = true;
		}
	}
	
	@Test
	public void testComplete() throws InterruptedException {
		TestClass app = new TestClass();
		Components.start(app);
		Event<?> test1 = new TestEvent1();
		app.fire(test1);
		synchronized (test1) {
			while (!handled1) {
				test1.wait(1000);
				assertTrue(handled1);
			}
		}
		while (app.counter > 0) {
			assertTrue(!test1.isDone());
			app.counter -= 1;
			synchronized (app) {
				app.notifyAll();
			}
		}
		test1.get();
		assertTrue(app.testDone);
	}

}
