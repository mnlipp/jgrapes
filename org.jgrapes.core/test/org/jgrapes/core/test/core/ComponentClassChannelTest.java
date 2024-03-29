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

import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.ComponentManager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class ComponentClassChannelTest {

    public static class MyChannel extends ClassChannel {
    }

    public static class ComponentWithClassChannel implements ComponentType {

        @ComponentManager(channel = MyChannel.class)
        private Manager manager;

        public int count = 0;

        @Handler(events = Start.class)
        public void onStarted(Event<?> event) {
            count += 1;
        }
    }

    @Test
    public void testClassChannel() throws InterruptedException {
        ComponentWithClassChannel app = new ComponentWithClassChannel();
        Components.manager(app).fire(new Start(), new MyChannel()).get();
        assertEquals(1, app.count);
    }

}
