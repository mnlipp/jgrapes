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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PostTest {

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

        public ContentProvider(Channel componentChannel) {
            super(componentChannel);
            contentProvider = this;
        }

        @RequestHandler(patterns = "/submit")
        public void onPost(Request.In.Post event, IOSubchannel channel)
                throws ParseException {
            invocations += 1;

            channel.setAssociated(ContentProvider.class, "submit");
            final HttpResponse response = event.httpRequest().response().get();
            response.setStatus(HttpStatus.OK);
            response.setHasPayload(true);
            response.setField(HttpField.CONTENT_TYPE,
                MediaType.builder().setType("text", "plain")
                    .setParameter("charset", "utf-8").build());
            channel.respond(new Response(response));
            event.setResult(true);
            event.stop();
        }

        @Handler
        public void onInput(Input<CharBuffer> event, IOSubchannel channel)
                throws InterruptedException {
            Optional<String> marker
                = channel.associated(ContentProvider.class, String.class);
            if (!marker.isPresent() || !marker.get().equals("submit")) {
                return;
            }
            ManagedBuffer<CharBuffer> outBuffer
                = channel.charBufferPool().acquire();
            outBuffer.backingBuffer().put(event.buffer().backingBuffer());
            channel.respond(
                Output.fromSink(outBuffer, event.isEndOfRecord()));
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
    public void testPost()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", server.getPort(), "/submit");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        conn.setDoOutput(true);
        OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
        osw.write("Hello!\n");
        osw.flush();
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String str = br.lines().findFirst().get();
            assertEquals("Hello!", str);
        }
        assertEquals(1, contentProvider.invocations);
    }

}
