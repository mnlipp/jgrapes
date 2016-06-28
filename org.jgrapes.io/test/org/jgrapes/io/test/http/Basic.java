package org.jgrapes.io.test.http;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Utils;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpServer;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.test.WaitFor;
import org.jgrapes.net.events.Ready;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Basic {

	static private TestServer server;
	
	public static class TestServer extends AbstractComponent {
		public InetSocketAddress addr;

		public TestServer() throws IOException, InterruptedException, 
				ExecutionException {
			attach(new NioDispatcher());
			attach(new HttpServer(getChannel(), null));
			WaitFor wf = new WaitFor
				(this, Ready.class, getChannel().getMatchKey());
			Utils.start(this);
			Ready readyEvent = (Ready) wf.get();
			if (!(readyEvent.getListenAddress() instanceof InetSocketAddress)) {
				fail();
			}
			addr = ((InetSocketAddress)readyEvent.getListenAddress());
		}
	}
	
	@BeforeClass
	static public void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new TestServer();
	}
	
	@AfterClass
	static public void stopServer() throws InterruptedException {
		server.fire(new Stop(), Channel.BROADCAST);
		Utils.awaitExhaustion();
		Utils.checkAssertions();
	}
	
	@Test
	public void testGetRoot() throws IOException, InterruptedException {
		URL url = new URL("http", server.addr.getAddress().getHostAddress(), 
				server.addr.getPort(), "/");
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
