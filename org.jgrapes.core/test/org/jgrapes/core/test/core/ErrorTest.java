/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.HandlingError;
import org.jgrapes.core.events.Start;
import static org.junit.Assert.*;
import org.junit.Test;

public class ErrorTest {

    public static class BuggyComponent extends Component {

        @Handler(events = Start.class)
        public void onStart(Event<?> evt) {
            throw new IllegalStateException();
        }
    }

    @Test
    public void testDefaultHandler() throws InterruptedException {
        PrintStream oldErr = System.err;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            System.setErr(new PrintStream(bos));
            BuggyComponent app = new BuggyComponent();
            Components.start(app);
            System.err.flush();
            String error = new String(bos.toByteArray());
            assertTrue(error.contains("IllegalStateException"));
        } finally {
            System.setErr(oldErr);
        }
        Components.awaitExhaustion();
    }

    public static class BuggyComponentWithHandler extends Component {

        public boolean caughtError = false;

        @Handler(events = Start.class)
        public void onStart(Event<?> evt) {
            throw new IllegalStateException();
        }

        @Handler(events = HandlingError.class, channels = Channel.class)
        public void onError(HandlingError evt) {
            if (evt.throwable().getClass() == IllegalStateException.class) {
                caughtError = true;
            }
        }
    }

    @Test
    public void testComplete() throws InterruptedException {
        BuggyComponentWithHandler app = new BuggyComponentWithHandler();
        Components.start(app);
        assertTrue(app.caughtError);
        Components.awaitExhaustion();
    }

}
