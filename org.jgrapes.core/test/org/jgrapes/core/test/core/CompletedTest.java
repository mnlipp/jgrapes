/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.test.core;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import static org.junit.Assert.*;
import org.junit.Test;

public class CompletedTest {

    private static boolean handled1 = false;

    public static class TestEvent1 extends Event<Integer> {

        /*
         * (non-Javadoc)
         * 
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
            synchronized (app) {
                app.counter -= 1;
                app.notifyAll();
            }
        }
        test1.get();
        assertTrue(app.testDone);
        Components.awaitExhaustion();
    }

}
