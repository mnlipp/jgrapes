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

package org.jgrapes.core.test.basic;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.Timer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SchedulerTest {
    static long t1 = 500;
    static long t2 = 1000;
    static long t3 = 1500;
    static long t4 = 2000;

    @BeforeAll
    static void init() {
        if ("true".equals(System.getenv().get("CI"))) {
            t1 = 2000;
            t2 = 4000;
            t3 = 6000;
            t4 = 8000;
        }
    }

    @Test
    void testBasic() throws InterruptedException {
        var hit1 = new AtomicBoolean(false);
        var hit2 = new AtomicBoolean(false);
        Instant startTime = Instant.now();
        Components.schedule(expiredTimer -> hit1.getAndSet(true),
            startTime.plusMillis(t1));
        Components.schedule(expiredTimer -> hit2.getAndSet(true),
            startTime.plusMillis(t2));
        Timer timer3
            = Components.schedule(expiredTimer -> hit1.getAndSet(false),
                startTime.plusMillis(t3));
        // Wait until "then" (between t1 and t2)
        Instant then = startTime.plusMillis((long) ((t1 + t2) / 2));
        Thread.sleep(Duration.between(Instant.now(), then).toMillis());
        assertTrue(hit1.get() && !hit2.get());

        // between t2 and t3
        then = startTime.plusMillis((long) ((t2 + t3) / 2));
        Thread.sleep(Duration.between(Instant.now(), then).toMillis());
        assertTrue(hit1.get() && hit2.get());

        // Cancel and wait
        timer3.cancel();
        // well after t3
        then = startTime.plusMillis(t4);
        Thread.sleep(Duration.between(Instant.now(), then).toMillis());
        assertTrue(hit1.get() && hit2.get());
    }
}