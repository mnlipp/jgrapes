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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapes.core.Component;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.test.WaitForTests;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.Server;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;
import org.junit.Test;

public class BigReadTest {

//	private static boolean localLogging = false;
//	
//	@BeforeClass
//	public static void enableLogging() throws FileNotFoundException {
//		Logger logger = Logger.getLogger("org.jgrapes");
//		if (logger.isLoggable(Level.FINE)) {
//			// Loggin already enabled
//			return;
//		}
//		localLogging = true;
//		System.setProperty("java.util.logging.SimpleFormatter.format",
//				"%1$tY-%1$tm-%1$td %5$s%n");
//		java.util.logging.Handler handler = new ConsoleHandler();
//		handler.setLevel(Level.FINEST);
//		handler.setFormatter(new SimpleFormatter());
//		logger.addHandler(handler);
//		logger.setUseParentHandlers(false);
//		logger.setLevel(Level.FINEST);
//	}
//
//	@AfterClass
//	public static void disableLogging() {
//		if (!localLogging) {
//			return;
//		}
//		System.setProperty("java.util.logging.SimpleFormatter.format",
//				"%1$tY-%1$tm-%1$td %5$s%n");
//		Logger logger = Logger.getLogger("org.jgrapes");
//		logger.setLevel(Level.INFO);
//		localLogging = false;		
//	}
	
	public class EchoServer extends Component {

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
		public void onAcctepted(Accepted<ManagedByteBuffer> event) 
				throws IOException, InterruptedException {
			try (ByteBufferOutputStream out = new ByteBufferOutputStream(
			        event.getConnection())) {
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
		WaitForTests wf = new WaitForTests
				(app, Ready.class, Server.DEFAULT_CHANNEL.getMatchKey());
		Components.start(app);
		Ready readyEvent = (Ready) wf.get();
		if (!(readyEvent.getListenAddress() instanceof InetSocketAddress)) {
			fail();
		}
		InetSocketAddress serverAddr 
			= ((InetSocketAddress)readyEvent.getListenAddress());

		// Watchdog
		final Thread mainTread = Thread.currentThread();
		final Thread watchdog = new Thread() {
			@Override
			public void run() {
				try {
					mainTread.join(10000);
					if (mainTread.isAlive()) {
						System.err.println("Watchdog kills main thread.");
						mainTread.interrupt();
					}
				} catch (InterruptedException e) {
				}
			}
		};
		
		AtomicInteger expected = new AtomicInteger(0);
		try (Socket client = new Socket(serverAddr.getAddress(),
		        serverAddr.getPort())) {
			watchdog.start();
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
		} finally {
			watchdog.interrupt();
		}
		assertEquals(1000000, expected.get());
		
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
