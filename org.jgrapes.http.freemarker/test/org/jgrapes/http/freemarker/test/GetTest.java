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

package org.jgrapes.http.freemarker.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.freemarker.FreeMarkerRequestHandler;
import org.jgrapes.io.IOSubchannel;

import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetTest {

	public static class TestServer extends BasicTestServer {

		public TestServer()
		        throws IOException, InterruptedException, ExecutionException {
			super(GetRequest.class);
		}

	}
	
	private static TestServer server;
	
	public static class ContentProvider extends FreeMarkerRequestHandler {

		public ContentProvider(Channel componentChannel, 
				ClassLoader contentLoader, String contentPath,
		        String prefix) {
			super(componentChannel, contentLoader, contentPath, prefix);
		}

		@RequestHandler(patterns="/generated/**")
		public void onGet(GetRequest event, IOSubchannel channel)
				throws ParseException {
			doGet(event, channel);
		}
	}
	
	@BeforeClass
	public static void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new TestServer();
		server.attach(new ContentProvider(server.channel(), 
				GetTest.class.getClassLoader(),	"templates", "/generated"));
		Components.start(server);
	}
	
	@AfterClass
	public static void stopServer() throws InterruptedException {
		server.fire(new Stop(), Channel.BROADCAST);
		Components.awaitExhaustion();
		Components.checkAssertions();
	}
	
	@Test(timeout=2500)
	public void testGetSimple() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", "localhost", server.getPort(),
		        "/generated/simple.ftl.html");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(2000);
		conn.setReadTimeout(2000);
		conn.setConnectTimeout(2000);
		conn.setReadTimeout(2000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Result is 42.", str);
		}
	}
	
	@Test(timeout=1500)
	public void testGetStatic() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", "localhost", server.getPort(),
		        "/generated/Readme.txt");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Test files.", str);
		}
	}
	
}
