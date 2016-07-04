package org.jgrapes.io.test.http;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Utils;
import org.jgrapes.core.events.Stop;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UnsupportedProtocolTests {

	static private BasicTestServer server;
	
	@BeforeClass
	static public void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new BasicTestServer();
		Utils.start(server);
	}
	
	@AfterClass
	static public void stopServer() throws InterruptedException {
		server.fire(new Stop(), Channel.BROADCAST);
		Utils.awaitExhaustion();
		Utils.checkAssertions();
	}
	
	@Test
	public void testGetRoot() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", server.getAddress().getHostAddress(), 
				server.getPort(), "/");
		Thread reader = Thread.currentThread();
		(new Thread() {
			@Override
			public void run() {
				try {
					reader.join(1000);
					if (reader.isAlive()) {
						reader.interrupt();
					}
				} catch (InterruptedException e) {
				}
			}
		}).start();
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		try {
			conn.getInputStream();
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage().indexOf(" 501 ") > 0);
		}
	}

}
