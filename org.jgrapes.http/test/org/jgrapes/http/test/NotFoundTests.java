package org.jgrapes.http.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.events.GetRequest;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class NotFoundTests {

	public static class TestServer extends BasicTestServer {

		public TestServer()
		        throws IOException, InterruptedException, ExecutionException {
			super(GetRequest.class);
		}

	}
	
	private static TestServer server;
	
	@BeforeClass
	public static void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new TestServer();
		Components.start(server);
	}
	
	@AfterClass
	public static void stopServer() throws InterruptedException {
		server.fire(new Stop(), Channel.BROADCAST);
		Components.awaitExhaustion();
		Components.checkAssertions();
	}
	
	@Test
	public void testGetRoot() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", server.getAddress().getHostAddress(), 
				server.getPort(), "/");
		Thread reader = Thread.currentThread();
		final Thread watchdog = new Thread() {
			@Override
			public void run() {
				try {
					reader.join(1000);
					if (reader.isAlive()) {
						reader.interrupt();
					}
				} catch (InterruptedException e) {
					// Okay
				}
			}
		};
		try {
			watchdog.start();
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(1000);
			conn.getInputStream();
			fail();
		} catch (FileNotFoundException e) {
			// Expected
		} finally {
			watchdog.interrupt();
		}
	}

}
