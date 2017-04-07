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

package org.jgrapes.io.test.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.test.WaitForTests;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Ready;

import static org.junit.Assert.*;
import org.junit.Test;

public class EchoTest {

	public class EchoServer extends Component {

		/**
		 * @throws IOException 
		 */
		public EchoServer() throws IOException {
			super();
			attach(new TcpServer(this));
		}

		@Handler
		public void onRead(Input<ManagedByteBuffer> event)
				throws InterruptedException {
			for (IOSubchannel channel: event.channels(IOSubchannel.class)) {
				ManagedByteBuffer out = channel.bufferPool().acquire();
				out.put(event.buffer());
				channel.respond(new Output<>(out, event.isEndOfRecord()));
			}
		}
	}

	@Test
	public void test() throws IOException, InterruptedException, 
			ExecutionException {
		EchoServer app = new EchoServer();
		app.attach(new NioDispatcher());
		WaitForTests wf = new WaitForTests(
				app, Ready.class, app.defaultCriterion());
		Components.start(app);
		Ready readyEvent = (Ready) wf.get();
		if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
			fail();
		}
		InetSocketAddress serverAddr 
			= ((InetSocketAddress)readyEvent.listenAddress());
		try (Socket client = new Socket(serverAddr.getAddress(),
		        serverAddr.getPort())) {
			final AtomicInteger expected = new AtomicInteger(0);
			Thread receiver = new Thread() {
				public void run() {
					InputStream fromServer;
					try {
						fromServer = client.getInputStream();
						BufferedReader in = new BufferedReader(
						        new InputStreamReader(fromServer, "ascii"));
						while (expected.get() < 16) {
							String line = in.readLine();
							assertNotEquals(null, line);
							String[] parts = line.split(":");
							assertEquals(expected.get(), 
										Integer.parseInt(parts[0]));
							assertEquals("Hello World!", parts[1]);
							expected.incrementAndGet();
						}
					} catch (IOException e) {
						// Ignored
					}
				}
			};
			receiver.start();

			Thread sender = new Thread() {
				@Override
				public void run() {
					try {
						OutputStream toServer = client.getOutputStream();
						for (int i = 0; i < 16; i++) {
							String line = i + ":Hello World!\n";
							toServer.write(line.getBytes("ascii"));
							toServer.flush();
							try {
								// If we're too fast, data will be appended
								// to previous, not yet flushed out chunk
								Thread.sleep(5);
							} catch (InterruptedException e) {
								// Ignored
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			sender.start();

			receiver.join(5000);
			assertEquals(16, expected.get());
		}
	
		Components.manager(app).fire(new Stop(), Channel.BROADCAST);
		long waitEnd = System.nanoTime() + 3000;
		while (true) {
			long waitTime = waitEnd - System.nanoTime();
			if (waitTime <= 0) {
				fail();
			}
			try {
				assertTrue(Components.awaitExhaustion(waitTime));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		}
	}

}
