package org.jgrapes.http.test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Stop;

import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class UnsupportedProtocolTests {

	private static BasicTestServer server;
	
	@BeforeClass
	public static void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new BasicTestServer();
		Components.start(server);
	}
	
	@AfterClass
	public static void stopServer() throws InterruptedException {
		server.fire(new Stop(), Channel.BROADCAST);
		Components.awaitExhaustion();
		Components.checkAssertions();
	}
	
	@Test(timeout=1500)
	public void testGetRoot() 
			throws IOException, InterruptedException, ExecutionException {
		try {
			URL url = new URL("http", "localhost", server.getPort(), "/");
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(1000);
			conn.getInputStream();
			fail();
		} catch (IOException e) {
			assertTrue(e.getMessage().indexOf(" 501 ") > 0);
		}
	}

}
