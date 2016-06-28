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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Utils;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.test.WaitFor;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.net.Server;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;
import org.junit.Test;

public class BigReadTest {

	public class EchoServer extends AbstractComponent {

		/**
		 * @throws IOException 
		 */
		public EchoServer() throws IOException {
			super(Server.DEFAULT_CHANNEL);
			attach(new Server(null));
		}

		/**
		 * Sends a lot of data to make sure that the data cannot be sent
		 * with a single write. Only then will the selector generate write
		 * the ops that we want to test here. 
		 * 
		 * @param event
		 * @throws IOException
		 * @throws InterruptedException
		 */
		@Handler
		public void onAcctepted(Accepted<ByteBuffer> event) 
				throws IOException, InterruptedException {
			EventPipeline pipeline = newEventPipeline();
			try (ByteBufferOutputStream out = new ByteBufferOutputStream
					(event.getConnection(), pipeline)) {
				for (int i = 0; i < 1000000; i++) {
					out.write(new String(i + ":Hello World!\n").getBytes());
				}
			}
		}
	}

	@Test
	public void test() throws IOException, InterruptedException, 
			ExecutionException {
		EchoServer app = new EchoServer();
		app.attach(new NioDispatcher());
		WaitFor wf = new WaitFor
				(app, Ready.class, Server.DEFAULT_CHANNEL.getMatchKey());
		Utils.start(app);
		Ready readyEvent = (Ready) wf.get();
		if (!(readyEvent.getListenAddress() instanceof InetSocketAddress)) {
			fail();
		}
		InetSocketAddress serverAddr 
			= ((InetSocketAddress)readyEvent.getListenAddress());
		
		AtomicInteger expected = new AtomicInteger(0);
		Thread receiver = new Thread() {
			@Override
			public void run() {
				try (Socket client = new Socket(serverAddr.getAddress(),
				        serverAddr.getPort())) {
					InputStream fromServer = client.getInputStream();
					BufferedReader in = new BufferedReader(
					        new InputStreamReader(fromServer, "ascii"));
					while (expected.get() < 1000000) {
						String line = in.readLine();
						String[] parts = line.split(":");
						assertEquals(expected.get(),
									Integer.parseInt(parts[0]));
						assertEquals("Hello World!", parts[1]);
						expected.incrementAndGet();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		receiver.start();
		receiver.join(2000);
		assertEquals(1000000, expected.get());
		
		Utils.manager(app).fire(new Stop(), Channel.BROADCAST);
		assertTrue(Utils.awaitExhaustion(3000));
	}

}
