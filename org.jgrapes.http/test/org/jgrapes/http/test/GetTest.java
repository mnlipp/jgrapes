/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2017  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.http.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.text.ParseException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.Request;
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
            super(Request.In.Get.class);
        }

    }

    private static TestServer server;
    private static ContentProvider contentProvider;

    public static class ContentProvider extends Component {

        public int invocations = 0;
        public ClassLoader jarLoader;

        public ContentProvider(Channel componentChannel) {
            super(componentChannel);
            contentProvider = this;
            jarLoader = new URLClassLoader(new URL[] {
                GetTest.class.getResource("/static-content.jar") });
            RequestHandler.Evaluator.add(this, "getDynamic", "/dynamic");
        }

        @RequestHandler(patterns = "*://*/top,/top/**")
        public void getTop(Request.In.Get event, IOSubchannel channel)
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
            event.setResult(true);
            event.stop();
        }

        @RequestHandler(patterns = "/not-found-status-only")
        public void getNotFoundStatusOnly(Request.In.Get event,
                IOSubchannel channel) throws ParseException {
            invocations += 1;

            ResponseCreationSupport.sendResponse(
                event.httpRequest(), channel, HttpStatus.NOT_FOUND);
            // Deliberately omit setting the result.
            event.stop();
        }

        @RequestHandler(dynamic = true)
        public void getDynamic(Request.In.Get event, IOSubchannel channel)
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
            event.setResult(true);
            event.stop();
        }

        @RequestHandler(patterns = "/static-content/**")
        public void getStaticContent(Request.In.Get event, IOSubchannel channel)
                throws ParseException {
            invocations += 1;

            ResponseCreationSupport.sendStaticContent(event, channel,
                path -> GetTest.class.getResource(path), null);
        }

        @RequestHandler(patterns = "/from-jar/**")
        public void getFromJar(Request.In.Get event, IOSubchannel channel)
                throws ParseException {
            invocations += 1;

            ResponseCreationSupport.sendStaticContent(event, channel,
                path -> jarLoader.getResource("only-in-jar/"
                    + ResourcePattern.removeSegments(path, 2)),
                null);
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

    @Test(timeout = 1500)
    public void testNoGetRoot()
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

    @Test(timeout = 1500)
    public void testNotFoundStatusOnly()
            throws IOException, InterruptedException, ExecutionException {
        try {
            URL url = new URL("http", "localhost", server.getPort(),
                "/not-found-status-only");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.getInputStream();
            fail();
        } catch (FileNotFoundException e) {
            // Expected
        }
        assertEquals(1, contentProvider.invocations);
    }

    @Test(timeout = 1500)
    public void testGetMatchTop()
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

    @Test(timeout = 1500)
    public void testGetMatchTopPlus()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", server.getPort(), "/top/plus");
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        conn.getInputStream();
        assertEquals(1, contentProvider.invocations);
    }

    @Test(timeout = 1500)
    public void testGetMatchDynamic()
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

    @Test(timeout = 2500)
    public void testGetUnversionedStatic()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", server.getPort(),
            "/static-content/index.html");
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.connect();
        String field = conn.getHeaderField("Cache-Control");
        ((HttpURLConnection) conn).disconnect();
        assertEquals("max-age=60", field);

        assertEquals(1, contentProvider.invocations);
    }

    @Test(timeout = 2500)
    public void testGetVersionedStatic()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", server.getPort(),
            "/static-content/versioned-1.0.0.html");
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.connect();
        String field = conn.getHeaderField("Cache-Control");
        ((HttpURLConnection) conn).disconnect();
        assertEquals("max-age=31536000", field);

        assertEquals(1, contentProvider.invocations);
    }

    @Test(timeout = 2500)
    public void testGetFromJar()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", server.getPort(),
            "/from-jar/static-content/index.html");
        // Unconditional fetch
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setUseCaches(false);
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            assertTrue(br.lines().anyMatch(l -> l.contains("<head>")));
        }
        ((HttpURLConnection) conn).disconnect();
        // Conditional fetch
        conn = url.openConnection();
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        conn.setIfModifiedSince(Instant.now().toEpochMilli());
        conn.setUseCaches(false);
        conn.connect();
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED,
            ((HttpURLConnection) conn).getResponseCode());
        ((HttpURLConnection) conn).disconnect();

        assertEquals(2, contentProvider.invocations);
    }

}
