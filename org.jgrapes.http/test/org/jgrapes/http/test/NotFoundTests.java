/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
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
		} catch (FileNotFoundException e) {
			// Expected
		}
	}

}
