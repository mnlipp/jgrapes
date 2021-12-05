/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class SelfTest {

    public static class TestComponent extends Component {

        public int count = 0;

        /**
         * @param componentChannel
         */
        public TestComponent() {
            super(new NamedChannel("test"));
        }

        @Handler(channels = Self.class)
        public void onStarted(Start event) {
            count += 1;
        }
    }

    @Test
    public void testStarted() throws InterruptedException {
        TestComponent app = new TestComponent();
        Components.manager(app).newSyncEventPipeline().fire(new Start(), app);
        assertEquals(1, app.count);
    }

}
