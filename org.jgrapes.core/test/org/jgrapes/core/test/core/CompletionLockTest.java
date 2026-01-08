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

import java.time.Duration;
import java.time.Instant;
import org.jgrapes.core.Channel;
import org.jgrapes.core.CompletionLock;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import org.jgrapes.core.events.Started;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CompletionLockTest {

    public static class App extends Component {

        public Start startEvent;
        public CompletionLock lock;
        private long timeout = 0;
        boolean started = false;
        public Instant startedAt = null;

        App(long timeout) {
            this.timeout = timeout;
        }

        @Handler
        public void onStart(Start event) {
            startEvent = event;
            startedAt = Instant.now();
            if (timeout >= 0) {
                this.lock = new CompletionLock(event, timeout);
            }
        }

        @Handler
        public void onStarted(Started event) {
            started = true;
        }
    }

    @Test
    public void testCompletionLock() throws InterruptedException {
        // Start App without lock and wait
        App app = new App(-1);
        app.fire(new Start(), Channel.BROADCAST);
        Thread.sleep(500);
        assertTrue(app.started);

        // Start App with lock
        Components.awaitExhaustion();
        app = new App(0);
        app.fire(new Start(), Channel.BROADCAST);
        Components.awaitExhaustion();
        assertFalse(app.started);

        // Remove lock
        app.lock.remove();
        Thread.sleep(500);
        assertTrue(app.started);

        // Start App with timeout lock
        app = new App(750);
        app.fire(new Start(), Channel.BROADCAST);
        while (app.startedAt == null) {
            Thread.sleep(10);
        }
        Thread.sleep(Duration.between(app.startedAt,
            Instant.now().plusMillis(500)).toMillis());
        assertFalse(app.started);
        Components.awaitExhaustion(1500);
        Instant completedAt = Instant.now();
        assertTrue(
            Duration.between(app.startedAt, completedAt).toMillis() < 1500);
    }
}