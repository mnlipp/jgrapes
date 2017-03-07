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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.FileOpened;
import org.jgrapes.io.events.Opened;
import org.jgrapes.io.events.SaveOutput;
import org.jgrapes.io.util.ByteBufferOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Michael N. Lipp
 *
 */
public class FileOutputWriteTests {

	public static class Producer extends Component {

		@Handler
		public void onOpened(FileOpened event)
		        throws InterruptedException, IOException {
			for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
				try (ByteBufferOutputStream out = new ByteBufferOutputStream(
				        channel, channel.getResponsePipeline())) {
					for (int i = 1; i <= 100; i++) {
						out.write(
						        new String(i + ": Hello World!\n").getBytes());
					}
				}
			}
		}
	}

	public static class StateChecker extends Component {
		
		public enum State { NEW, OPENED, CLOSED }
		
		public State state = State.NEW;

		public StateChecker() {
			super(Channel.BROADCAST);
		}
		
		@Handler
		public void opened(Opened event) {
			assertTrue(state == State.NEW);
			state = State.OPENED;
		}
		
		@Handler
		public void closed(Closed event) {
			assertTrue(state == State.OPENED);
			state = State.CLOSED;
		}
	}
	
	@Test
	public void testWrite()
	        throws IOException, InterruptedException, ExecutionException {
		Path filePath = Files.createTempFile("jgrapes-", ".txt");
		filePath.toFile().deleteOnExit();
		Producer producer = new Producer();
		FileStorage app = new FileStorage(producer, 512);
		app.attach(producer);
		final StateChecker sc = app.attach(new StateChecker());
		Components.start(app);
		app.fire(new SaveOutput(filePath, StandardOpenOption.WRITE),
		        IOSubchannel.defaultInstance(producer));
		Components.awaitExhaustion();
		try (BufferedReader br = new BufferedReader(
		        new FileReader(filePath.toFile()))) {
			int expect = 1;
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				int num = Integer.parseInt(line.split(":")[0]);
				assertEquals(expect, num);
				expect += 1;
			}
			assertEquals(101, expect);
		}
		assertEquals(StateChecker.State.CLOSED, sc.state);
		Components.checkAssertions();
	}
}
