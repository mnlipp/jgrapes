/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrapes.io.test.file;

import static org.junit.Assert.*;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.Connection;
import org.jgrapes.io.Server;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Ready;
import org.junit.Test;

public class ServerStateTest {

	public Connection serverConnection = null;
	
	public enum State { NEW, READY, CLOSING, CLOSED };
	
	public class StateChecker extends AbstractComponent {
		
		public State state = State.NEW;

		public StateChecker() {
			super(Channel.BROADCAST);
		}
		
		@Handler
		public void onReady(Ready event) {
			assertTrue(state == State.NEW);
			state = State.READY;
			serverConnection = event.getConnection();
		}
		
		@Handler
		public void onClose(Close<?> event) {
			assertTrue(state == State.READY);
			state = State.CLOSING;
		}
		
		@Handler
		public void onClosed(Closed<?> event) {
			assertTrue(state == State.CLOSING);
			state = State.CLOSED;
		}
	}
	
	@Test
	public void testStartClose() throws InterruptedException {
		Server app = new Server(null);
		StateChecker checker = new StateChecker();
		app.attach(checker);
		Utils.start(app);
		Utils.awaitExhaustion();
		assertEquals(State.READY, checker.state);
		Utils.manager(app).fire
			(new Close<>(serverConnection), Channel.BROADCAST);
		Utils.awaitExhaustion();
		assertEquals(State.CLOSED, checker.state);
	}

//	@Test
//	public void testStartStop() throws InterruptedException {
//		Server app = new Server(null);
//		StateChecker checker = new StateChecker();
//		app.attach(checker);
//		Utils.start(app);
//		Utils.awaitExhaustion();
//		assertEquals(State.READY, checker.state);
//		Utils.manager(app).fire(new Stop(), Channel.BROADCAST);
//		Utils.awaitExhaustion();
//		assertEquals(State.CLOSED, checker.state);
//	}

}
