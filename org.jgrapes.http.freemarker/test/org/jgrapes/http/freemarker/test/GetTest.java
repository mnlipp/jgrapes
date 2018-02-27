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

import org.junit.After;

import static org.junit.Assert.*;

import org.junit.Before;
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

		@RequestHandler(patterns="/**")
		public void onGet(GetRequest event, IOSubchannel channel)
				throws ParseException {
			doGet(event, channel);
		}
	}
	
	@Before
	public void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new TestServer();
		Components.start(server);
	}
	
	@After
	public void stopServer() throws InterruptedException {
		server.fire(new Stop(), Channel.BROADCAST);
		Components.awaitExhaustion();
		Components.checkAssertions();
	}
	
	@Test(timeout=5000)
	public void testGetSimpleWithoutPrefix() 
			throws IOException, InterruptedException, ExecutionException {
		server.attach(new ContentProvider(server.channel(), 
				GetTest.class.getClassLoader(),	"templates", "/"));
		URL url = new URL("http", "localhost", server.getPort(),
		        "/simple.ftl.html");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Result is 42.", str);
		}
	}
	
	@Test(timeout=5000)
	public void testGetStaticWithoutPrefix() 
			throws IOException, InterruptedException, ExecutionException {
		server.attach(new ContentProvider(server.channel(), 
				GetTest.class.getClassLoader(),	"templates", "/"));
		URL url = new URL("http", "localhost", server.getPort(),
		        "/Readme.txt");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Test files.", str);
		}
	}
	
	@Test(timeout=5000)
	public void testGetSimpleWithPrefix() 
			throws IOException, InterruptedException, ExecutionException {
		server.attach(new ContentProvider(server.channel(), 
				GetTest.class.getClassLoader(),	"templates", "/generated"));
		URL url = new URL("http", "localhost", server.getPort(),
		        "/generated/simple.ftl.html");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Result is 42.", str);
		}
	}
	
	@Test(timeout=5000)
	public void testGetStaticWithPrefix() 
			throws IOException, InterruptedException, ExecutionException {
		server.attach(new ContentProvider(server.channel(), 
				GetTest.class.getClassLoader(),	"templates", "/generated"));
		URL url = new URL("http", "localhost", server.getPort(),
		        "/generated/Readme.txt");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Test files.", str);
		}
	}
	
}
