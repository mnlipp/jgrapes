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
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.NamedEvent;
import org.jgrapes.core.annotation.Handler;
import static org.junit.Assert.*;
import org.junit.Test;

public class SuspendTests {

    public static class TestApp extends Component {

        public String sequence = "";
        private Event<?> suspended;

        public TestApp() {
        }

        @Handler(namedEvents = "Test", priority = 1000)
        public void onTest1(NamedEvent<Void> event) {
            sequence += "1";
            event.suspendHandling();
            suspended = event;
        }

        @Handler(namedEvents = "1stAfterTest", priority = 900)
        public void onAfterTest1(NamedEvent<Void> event) {
            sequence += ", A1";
            suspended.resumeHandling();
        }

        @Handler(namedEvents = "Test", priority = 900)
        public void onTest2(NamedEvent<Void> event) {
            sequence += ", 2";
        }

        @Handler(namedEvents = "Test", priority = 800)
        public void onTest3(NamedEvent<Void> event) {
            sequence += ", 3";
            event.suspendHandling();
            suspended = event;
        }

        @Handler(namedEvents = "2ndAfterTest", priority = 900)
        public void onAfterTest2(NamedEvent<Void> event) {
            sequence += ", A2";
            suspended.resumeHandling();
        }

        @Handler(namedEvents = "Test", priority = 700)
        public void onTest4(NamedEvent<Void> event) {
            sequence += ", 4";
            event.suspendHandling(() -> {
                sequence += ", 4a";
            });
            suspended = event;
        }

        @Handler(namedEvents = "3rdAfterTest", priority = 900)
        public void onAfterTest3(NamedEvent<Void> event) {
            sequence += ", A3";
            suspended.resumeHandling();
        }

        @Handler(namedEvents = "Test", priority = 600)
        public void onTest5(NamedEvent<Void> event) {
            sequence += ", 5";
        }
    }

    @Test
    public void testSuspend() throws InterruptedException {
        TestApp app = new TestApp();
        EventPipeline me = app.newEventPipeline();
        me.fire(new NamedEvent<Void>("Test"), app);
        me.fire(new NamedEvent<Void>("1stAfterTest"), app);
        me.fire(new NamedEvent<Void>("2ndAfterTest"), app);
        Components.start(app);
        Thread.sleep(500);
        me.fire(new NamedEvent<Void>("3rdAfterTest"), app);
        Components.awaitExhaustion();
        assertEquals("1, A1, 2, 3, A2, 4, A3, 4a, 5", app.sequence);
    }

}
