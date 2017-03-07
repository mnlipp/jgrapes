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
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpMediaTypeField;
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

		@RequestHandler(patterns="*://*/top/**")
		public void getTop(GetRequest event) throws ParseException {
			invocations += 1;
			final IOSubchannel channel = event.firstChannel(IOSubchannel.class);
			
			final HttpResponse response = event.getRequest().getResponse().get();
			response.setStatus(HttpStatus.OK);
			response.setMessageHasBody(true);
			HttpMediaTypeField media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "plain");
			media.setParameter("charset", "utf-8");
			response.setField(media);
			fire(new Response(response), channel);
			try {
				fire(Output.wrap("Top!".getBytes("utf-8"), true), channel);
			} catch (UnsupportedEncodingException e) {
				// Supported by definition
			}
			event.stop();
		}
		
		@RequestHandler(dynamic=true)
		public void getDynamic(GetRequest event) throws ParseException {
			invocations += 1;
			final IOSubchannel channel = event.firstChannel(IOSubchannel.class);
			
			final HttpResponse response = event.getRequest().getResponse().get();
			response.setStatus(HttpStatus.OK);
			response.setMessageHasBody(true);
			HttpMediaTypeField media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "plain");
			media.setParameter("charset", "utf-8");
			response.setField(media);
			fire(new Response(response), channel);
			try {
				fire(Output.wrap("Dynamic!".getBytes("utf-8"), true), channel);
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
		server.attach(new ContentProvider(server.getChannel()));
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
		assertEquals(0, contentProvider.invocations);
	}
	
	@Test
	public void testGetTop() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", server.getAddress().getHostAddress(), 
				server.getPort(), "/top");
		Thread reader = Thread.currentThread();
		Thread watchdog = new Thread() {
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
			try (BufferedReader br = new BufferedReader(
			        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
				String str = br.lines().findFirst().get();
				assertEquals("Top!", str);
			}
		} finally {
			watchdog.interrupt();
		}
		assertEquals(1, contentProvider.invocations);
	}

	@Test
	public void testGetTopPlus() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", server.getAddress().getHostAddress(), 
				server.getPort(), "/top/plus");
		Thread reader = Thread.currentThread();
		Thread watchdog = new Thread() {
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
		} finally {
			watchdog.interrupt();
		}
		assertEquals(1, contentProvider.invocations);
	}

	@Test
	public void testGetDynamic() 
			throws IOException, InterruptedException, ExecutionException {
		URL url = new URL("http", server.getAddress().getHostAddress(), 
				server.getPort(), "/dynamic");
		Thread reader = Thread.currentThread();
		Thread watchdog = new Thread() {
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
			try (BufferedReader br = new BufferedReader(
			        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
				String str = br.lines().findFirst().get();
				assertEquals("Dynamic!", str);
			}
		} finally {
			watchdog.interrupt();
		}
		assertEquals(1, contentProvider.invocations);
	}

}
