package org.jgrapes.core.test.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.junit.Test;

public class CompletedTest {

	private static AtomicBoolean handled1 = new AtomicBoolean();
	
	public static class TestEvent1 extends Event<Integer> {

		/* (non-Javadoc)
		 * @see org.jgrapes.core.internal.EventBase#handled()
		 */
		@Override
		protected void handled() {
			synchronized (handled1) {
				handled1.set(true);
				handled1.notifyAll();
			}
		}
		
	}
	
	public static class TestEvent2 extends Event<Integer> {
	}
	
	public static class TestClass extends Component {
		
		public AtomicInteger counter = new AtomicInteger(100);
		public boolean testDone = false;
		
		@Handler
		public void onTest1(TestEvent1 evt) {
			newEventPipeline().fire(new TestEvent2());
		}

		@Handler
		public void onTest2(TestEvent2 evt) throws InterruptedException {
			synchronized (counter) {
				while (counter.get() > 0) {
					counter.wait();
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
		synchronized (handled1) {
			while (!handled1.get()) {
				handled1.wait(1000);
				assertTrue(handled1.get());
			}
		}
		while (app.counter.get() > 0) {
			assertTrue(!test1.isDone());
			app.counter.decrementAndGet();
			synchronized (app.counter) {
				app.counter.notifyAll();
			}
		}
		test1.get();
		assertTrue(app.testDone);
	}

}
