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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.StringList;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpConnector;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.HostUnresolved;
import org.jgrapes.http.events.HttpConnected;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.ConnectError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.CharBufferWriter;
import org.jgrapes.net.SocketConnector;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientTest {

    public static class TestServer extends BasicTestServer {

        public TestServer() throws Exception {
            super(Request.In.Get.class);
        }

    }

    public static class TopProvider extends Component {

        public TopProvider(Channel componentChannel) {
            super(componentChannel);
        }

        @RequestHandler(patterns = "*://*/top,/top/**")
        public void getTop(Request.In.Get event, IOSubchannel channel)
                throws ParseException {

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

    }

    public static class ErrorReceived extends Event<Object> {
    }

    public static class ResponseReceived extends Event<Object> {
    }

    public static class InputReceived extends Event<Object> {
    }

    public static class ClosedReceived extends Event<Object> {
    }

    public static class SendToServer extends Event<Object> {
        private String data;

        public SendToServer(String data) {
            this.data = data;
        }

        public String data() {
            return data;
        }
    }

    public static class CloseConnection extends Event<Void> {
    }

    public static class TestClient extends Component {

        public IOSubchannel upChannel;
        public MessageHeader response;
        public String textResult;

        public TestClient() throws IOException {
            attach(new NioDispatcher());
            SocketConnector conn = attach(new SocketConnector(SELF));
            attach(new HttpConnector(this, conn));
        }

        public void startRequest(Request.Out event,
                Consumer<IOSubchannel> continuation) {
            event.setAssociated(this, continuation);
            fire(event);
        }

        @Handler
        public void onError(Error event) {
            fire(new ErrorReceived().setResult(event));
        }

        @SuppressWarnings("unchecked")
        @Handler
        public void onConnected(HttpConnected event, IOSubchannel channel) {
            event.request().associated(this, Consumer.class).ifPresent(
                continuation -> ((Consumer<IOSubchannel>) continuation)
                    .accept(channel));
        }

        @Handler
        public void onResponse(Response event, IOSubchannel channel) {
            channel.setAssociated(TestClient.class, this);
            upChannel = channel;
            response = event.response();
            if (!response.hasPayload()) {
                fire(new ResponseReceived());
            }
        }

        @Handler
        public void onInput(Input<?> event, IOSubchannel channel) {
            if (!channel.associated(TestClient.class).isPresent()) {
                return;
            }
            if (event.buffer().backingBuffer() instanceof CharBuffer) {
                textResult
                    = ((CharBuffer) event.buffer().backingBuffer()).toString();
            }
            if (event.isEndOfRecord()) {
                fire(new InputReceived());
            }
        }

        @Handler
        public void onClosed(Closed<?> event, IOSubchannel channel) {
            if (!channel.associated(TestClient.class).isPresent()) {
                return;
            }
            fire(new ClosedReceived());
        }

        @Handler
        public void onSendToServer(SendToServer event) {
            upChannel.respond(Output.from(event.data(), true));
        }

        @Handler
        public void onCloseConnection(CloseConnection event) {
            upChannel.respond(new Close());
        }
    }

    private static TestServer srvApp;
    private static TestClient clntApp;

    @BeforeClass
    public static void startApps() throws Exception {
        srvApp = new TestServer();
        srvApp.attach(new InMemorySessionManager(srvApp.channel()));
        srvApp.attach(new TopProvider(srvApp.channel()));
        srvApp.attach(new ReflectProvider(srvApp.channel()));
        srvApp.attach(new WsEchoProvider(srvApp.channel()));
        Components.start(srvApp);
        clntApp = new TestClient();
        Components.start(clntApp);
    }

    @AfterClass
    public static void stopApps() throws InterruptedException {
        srvApp.fire(new Stop(), Channel.BROADCAST);
        clntApp.fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

    @Test(timeout = 1500)
    public void testUnknownHost()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "never.ever.known", srvApp.getPort(), "/");
        WaitForTests<ErrorReceived> doneWaiter = new WaitForTests<>(clntApp,
            ErrorReceived.class, clntApp.defaultCriterion());
        clntApp.startRequest(new Request.Out.Get(url), channel -> {
        });
        ErrorReceived doneEvent = doneWaiter.get();
        assertEquals(HostUnresolved.class, doneEvent.get().getClass());
    }

    @Test(timeout = 1500)
    public void testConnectionRefused()
            throws IOException, InterruptedException, ExecutionException {
        // Though not impossible, it's highly unlikely to have a server here
        URL url = new URL("http", "localhost", srvApp.getPort() + 1, "/top");
        WaitForTests<ErrorReceived> doneWaiter = new WaitForTests<>(clntApp,
            ErrorReceived.class, clntApp.defaultCriterion());
        clntApp.startRequest(new Request.Out.Get(url), channel -> {
        });
        ErrorReceived doneEvent = doneWaiter.get();
        assertEquals(ConnectError.class, doneEvent.get().getClass());
    }

    @Test(timeout = 1500)
    public void testGetMatchTop()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", srvApp.getPort(), "/top");
        WaitForTests<InputReceived> done = new WaitForTests<>(clntApp,
            InputReceived.class, clntApp.defaultCriterion());
        clntApp.startRequest(new Request.Out.Get(url), channel -> {
        });
        done.get();
        assertEquals("Top!", clntApp.textResult);
        done = new WaitForTests<>(clntApp, InputReceived.class,
            clntApp.defaultCriterion());
        clntApp.fire(new Request.Out.Get(url));
        done.get();
    }

    @Test(timeout = 1500)
    public void testPost()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", srvApp.getPort(), "/reflect");
        Request.Out.Post post = new Request.Out.Post(url);
        post.httpRequest().setField("Content-Type",
            "text/plain; charset=utf-8");
        post.httpRequest().setHasPayload(true);
        WaitForTests<InputReceived> done = new WaitForTests<>(clntApp,
            InputReceived.class, clntApp.defaultCriterion());
        clntApp.startRequest(post, channel -> {
            try (CharBufferWriter out = new CharBufferWriter(channel)) {
                out.suppressClose();
                out.write("Hello!");
                // test with two output events
                for (int i = 0; i < channel.charBufferPool().bufferSize() * 1.1;
                        i++) {
                    out.write('.');
                }
                out.write("Full Stop!");
            }
        });
        done.get();
        assertTrue(clntApp.textResult.startsWith("->Hello!"));
        assertTrue(clntApp.textResult.endsWith("Full Stop!"));
    }

    @Test(timeout = 1500)
    public void testWsEcho()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", srvApp.getPort(),
            "/ws/echo?store=42");
        Request.Out.Get upgrade = new Request.Out.Get(url);
        upgrade.httpRequest().setField(HttpField.UPGRADE,
            new StringList("websocket"));
        upgrade.setConnectedCallback((request, connection) -> {
            request.httpRequest().setField("Origin", "null");
        });
        upgrade.httpRequest().setField("Sec-WebSocket-Version", "13");
        byte[] randomBytes = new byte[16];
        new Random().nextBytes(randomBytes);
        upgrade.httpRequest().setField("Sec-WebSocket-Key",
            Base64.getEncoder().encodeToString(randomBytes));

        // Expect greeting
        WaitForTests<?> done = new WaitForTests<>(clntApp,
            InputReceived.class, clntApp.defaultCriterion());
        clntApp.startRequest(upgrade, channel -> {
        });
        done.get();
        assertEquals("/Greetings!", clntApp.textResult);

        // Expect value stored in initial request (session has been "forwarded")
        done = new WaitForTests<>(clntApp,
            InputReceived.class, clntApp.defaultCriterion());
        clntApp.fire(new SendToServer("/stored"), clntApp);
        done.get();
        assertEquals("42", clntApp.textResult);

        // Expect echo
        done = new WaitForTests<>(clntApp,
            InputReceived.class, clntApp.defaultCriterion());
        clntApp.fire(new SendToServer("Hello!"), clntApp);
        done.get();
        assertEquals("Hello!", clntApp.textResult);

        // Expect closed
        done = new WaitForTests<>(clntApp,
            ClosedReceived.class, clntApp.defaultCriterion());
        clntApp.fire(new SendToServer("/quit"), clntApp);
        done.get();
        assertEquals("Hello!", clntApp.textResult);
        done.get();
    }

    @Test(timeout = 1500)
    public void testWsClientClose()
            throws IOException, InterruptedException, ExecutionException {
        URL url = new URL("http", "localhost", srvApp.getPort(), "/ws/echo");
        Request.Out.Get upgrade = new Request.Out.Get(url);
        upgrade.httpRequest().setField(HttpField.UPGRADE,
            new StringList("websocket"));
        upgrade.setConnectedCallback((request, connection) -> {
            request.httpRequest().setField("Origin", "null");
        });
        upgrade.httpRequest().setField("Sec-WebSocket-Version", "13");
        byte[] randomBytes = new byte[16];
        new Random().nextBytes(randomBytes);
        upgrade.httpRequest().setField("Sec-WebSocket-Key",
            Base64.getEncoder().encodeToString(randomBytes));

        // Expect greeting
        WaitForTests<?> done = new WaitForTests<>(clntApp,
            InputReceived.class, clntApp.defaultCriterion());
        clntApp.startRequest(upgrade, channel -> {
        });
        done.get();
        assertEquals("/Greetings!", clntApp.textResult);

        // Expect closed
        done = new WaitForTests<>(clntApp,
            ClosedReceived.class, clntApp.defaultCriterion());
        clntApp.fire(new CloseConnection(), clntApp);
        done.get();
    }
}
