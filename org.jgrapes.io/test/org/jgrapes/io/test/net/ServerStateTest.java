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

package org.jgrapes.io.test.net;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.test.WaitForTests;
import org.jgrapes.net.SocketServer;
import org.jgrapes.net.events.Ready;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ServerStateTest {

    public enum State {
        NEW, READY, CLOSING, CLOSED
    }

    public class StateChecker extends Component {

        public Channel serverChannel = null;

        public State state = State.NEW;

        public StateChecker() {
            super(Channel.BROADCAST);
        }

        @Handler
        public void onReady(Ready event) {
            assertTrue(state == State.NEW);
            state = State.READY;
            serverChannel = event.channels()[0];
        }

        @Handler
        public void onClose(Close event) {
            assertTrue(state == State.READY);
            state = State.CLOSING;
        }

        @Handler
        public void onClosed(Closed<?> event) {
            assertTrue(state == State.CLOSING);
            state = State.CLOSED;
        }

    }

    SocketServer app;
    StateChecker checker;

    @Before
    public void setUp() throws Exception {
        NioDispatcher root = new NioDispatcher();
        app = root.attach(new SocketServer());
        checker = new StateChecker();
        app.attach(checker);
        WaitForTests<Ready> wf
            = new WaitForTests<>(app, Ready.class, Channel.class);
        Components.start(app);
        wf.get();
    }

    @Test
    public void testStartClose() throws InterruptedException {
        assertEquals(State.READY, checker.state);
        Components.manager(app).fire(
            new Close(), app.channel()).get();
        assertEquals(State.CLOSED, checker.state);
        Components.manager(app).fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

    @Test
    public void testStartStop() throws InterruptedException {
        assertEquals(State.READY, checker.state);
        Components.manager(app).fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
        assertEquals(State.CLOSED, checker.state);
        Components.checkAssertions();
    }

}
