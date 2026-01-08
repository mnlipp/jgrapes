/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.test.core;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CacheTestTest {

    class App extends Component {
    }

    class TestEvent extends Event<Void> {
    }

    public static class Comp extends Component {

        public int testEvents = 0;

        public Comp(Channel channel) {
            super(channel);
        }

        @Handler
        public void onTest(TestEvent event) {
            testEvents += 1;
        }
    }

    @Test
    void testUpdateCache() throws InterruptedException {
        // App with component
        App app = new App();
        Comp comp1 = app.attach(new Comp(app));
        app.fire(new Start());

        // Fire event
        app.fire(new TestEvent(), app);
        Components.awaitExhaustion();
        assertEquals(1, comp1.testEvents);

        // Attach another component and fire again
        Comp comp2 = app.attach(new Comp(app));
        app.fire(new TestEvent(), app);
        Components.awaitExhaustion();
        assertEquals(2, comp1.testEvents);
        assertEquals(1, comp2.testEvents);

        // Detach component
        comp1.detach();
        app.fire(new TestEvent(), app);
        Components.awaitExhaustion();
        assertEquals(2, comp1.testEvents);
        assertEquals(2, comp2.testEvents);
    }

}