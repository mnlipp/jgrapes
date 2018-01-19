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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Output;

import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.Before;
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
	private static ContentProvider contentProvider;
	
	public static class ContentProvider extends Component {
		
		public int invocations = 0;
		
		public ContentProvider(Channel componentChannel) {
			super(componentChannel);
			contentProvider = this;
			RequestHandler.Evaluator.add(this, "getDynamic", "/dynamic");
		}

		@RequestHandler(patterns="*://*/top,/top/**")
		public void getTop(GetRequest event, IOSubchannel channel)
				throws ParseException {
			invocations += 1;
			
			final HttpResponse response = event.httpRequest().response().get();
			response.setStatus(HttpStatus.OK);
			response.setHasPayload(true);
			response.setField(HttpField.CONTENT_TYPE,
					MediaType.builder().setType("text", "plain")
					.setParameter("charset", "utf-8").build());
			fire(new Response(response), channel);
			try {
				fire(Output.from("Top!".getBytes("utf-8"), true), channel);
			} catch (UnsupportedEncodingException e) {
				// Supported by definition
			}
			event.stop();
		}
		
		@RequestHandler(dynamic=true)
		public void getDynamic(GetRequest event, IOSubchannel channel)
				throws ParseException {
			invocations += 1;
			
			final HttpResponse response = event.httpRequest().response().get();
			response.setStatus(HttpStatus.OK);
			response.setHasPayload(true);
			response.setField(HttpField.CONTENT_TYPE,
					MediaType.builder().setType("text", "plain")
					.setParameter("charset", "utf-8").build());
			fire(new Response(response), channel);
			try {
				fire(Output.from("Dynamic!".getBytes("utf-8"), true), channel);
			} catch (UnsupportedEncodingException e) {
				// Supported by definition
			}
			event.stop();
		}
		
	}
	
	@BeforeClass
	public static void startServer() throws IOException, InterruptedException, 
			ExecutionException {
		server = new TestServer();
		server.attach(new ContentProvider(server.channel()));
		Components.start(server);
	}
	
	@Before
	public void startTest() {
		contentProvider.invocations = 0;
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
		assertEquals(0, contentProvider.invocations);
	}
	
	@Test(timeout=1500)
	public void testGetTop() 
	        throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", "localhost", server.getPort(), "/top");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Top!", str);
		}
		assertEquals(1, contentProvider.invocations);
	}

	@Test(timeout=1500)
	public void testGetTopPlus() 
	        throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", "localhost", server.getPort(), "/top/plus");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		conn.getInputStream();
		assertEquals(1, contentProvider.invocations);
	}

	@Test(timeout=1500)
	public void testGetDynamic() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", "localhost", server.getPort(), "/dynamic");
		URLConnection conn = url.openConnection();
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		try (BufferedReader br = new BufferedReader(
		        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
			String str = br.lines().findFirst().get();
			assertEquals("Dynamic!", str);
		}
		assertEquals(1, contentProvider.invocations);
	}

}
