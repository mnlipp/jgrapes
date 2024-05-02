/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io.test.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Started;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.OpenSocketConnection;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.test.WaitForTests;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.net.SocketConnector;
import org.jgrapes.net.SocketServer;
import org.jgrapes.net.SslCodec;
import org.jgrapes.net.events.Connected;
import org.jgrapes.net.events.Ready;
import static org.junit.Assert.*;
import org.junit.Test;

public class EchoTest2 {

    public class EchoServer extends Component {

        /**
         * @throws IOException 
         */
        public EchoServer() throws IOException {
        }

        @Handler
        public void onRead(Input<ByteBuffer> event, IOSubchannel channel)
                throws InterruptedException {
            ManagedBuffer<ByteBuffer> out = channel.byteBufferPool().acquire();
            out.backingBuffer().put(event.data());
            channel.respond(Output.fromSink(out, event.isEndOfRecord()));
        }
    }

    public class Done extends Event<Void> {
    }

    public class ClientApp extends Component {
        private InetSocketAddress serverAddr;

        /**
         * @param serverAddr
         */
        public ClientApp(InetSocketAddress serverAddr) {
            this.serverAddr = serverAddr;
        }

        @Handler
        public void onStarted(Started event) throws InterruptedException {
            fire(new OpenSocketConnection(serverAddr));
        }

        @Handler
        public void onConnected(Connected<?> event, IOSubchannel channel)
                throws InterruptedException {
            channel.setAssociated(EchoTest2.class, true);
            ManagedBuffer<ByteBuffer> buf = channel.byteBufferPool().acquire();
            buf.backingBuffer().put("Hello World!".getBytes());
            channel.respond(Output.fromSink(buf, true));
        }

        @Handler
        public void onInput(Input<ByteBuffer> event, IOSubchannel channel) {
            if (!(channel.associated(EchoTest2.class, Boolean.class)
                .orElse(false))) {
                return;
            }
            String data
                = Charset.defaultCharset().decode(event.data()).toString();
            assertEquals("Hello World!", data);
            channel.respond(new Close());
        }

        @Handler
        public void onClosed(Closed<?> event, IOSubchannel channel) {
            fire(new Done());
        }
    }

    @Test
    public void testTcp() throws IOException, InterruptedException,
            ExecutionException, TimeoutException {
        // Create server
        EchoServer srvApp = new EchoServer();
        srvApp.attach(new SocketServer(srvApp));
        srvApp.attach(new NioDispatcher());
        WaitForTests<Ready> wf = new WaitForTests<>(
            srvApp, Ready.class, srvApp.defaultCriterion());
        Components.start(srvApp);
        Ready readyEvent = (Ready) wf.get();
        if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
            fail();
        }
        InetSocketAddress serverAddr = new InetSocketAddress("localhost",
            ((InetSocketAddress) readyEvent.listenAddress()).getPort());

        // Create client
        ClientApp clntApp = new ClientApp(serverAddr);
        clntApp.attach(new SocketConnector(clntApp));
        clntApp.attach(new NioDispatcher());
        WaitForTests<Done> done
            = new WaitForTests<>(clntApp, Done.class,
                clntApp.defaultCriterion());
        Components.start(clntApp);
        done.get();

        // Stop
        Components.manager(clntApp).fire(new Stop(), Channel.BROADCAST);
        Components.manager(srvApp).fire(new Stop(), Channel.BROADCAST);
        long waitEnd = System.currentTimeMillis() + 3000;
        while (true) {
            long waitTime = waitEnd - System.currentTimeMillis();
            if (waitTime <= 0) {
                fail();
            }
            Components.checkAssertions();
            try {
                assertTrue(Components.awaitExhaustion(waitTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

    @Test
    public void testSsl() throws IOException, InterruptedException,
            ExecutionException, TimeoutException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        // Create server
        EchoServer srvApp = new EchoServer();
        srvApp.attach(new NioDispatcher());

        // Create TLS "converter"
        KeyStore serverStore = KeyStore.getInstance("JKS");
        try (FileInputStream kf
            = new FileInputStream("test-resources/localhost.jks")) {
            serverStore.load(kf, "nopass".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverStore, "nopass".toCharArray());
        SSLContext sslSrvContext = SSLContext.getInstance("TLS");
        sslSrvContext.init(kmf.getKeyManagers(), null, new SecureRandom());

        // Create a TCP server for SSL
        SocketServer secSrvNetwork = srvApp.attach(new SocketServer());
        srvApp.attach(new SslCodec(srvApp, secSrvNetwork, sslSrvContext));

        // Server prepared, start it.
        WaitForTests<Ready> wf = new WaitForTests<>(
            secSrvNetwork, Ready.class, secSrvNetwork.defaultCriterion());
        Components.start(srvApp);
        Ready readyEvent = (Ready) wf.get();
        if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
            fail();
        }
        InetSocketAddress serverAddr = new InetSocketAddress("localhost",
            ((InetSocketAddress) readyEvent.listenAddress()).getPort());

        // Create client
        ClientApp clntApp = new ClientApp(serverAddr);
        clntApp.attach(new NioDispatcher());

        // Create a TCP connector for SSL
        SocketConnector secClntNetwork = clntApp.attach(new SocketConnector());
        clntApp.attach(new SslCodec(clntApp, secClntNetwork, true));
        WaitForTests<Ready> done
            = new WaitForTests<>(clntApp, Done.class,
                clntApp.defaultCriterion());
        Components.start(clntApp);
        done.get();

        // Stop
        Components.manager(clntApp).fire(new Stop(), Channel.BROADCAST);
        Components.manager(srvApp).fire(new Stop(), Channel.BROADCAST);
        long waitEnd = System.currentTimeMillis() + 300000;
        while (true) {
            long waitTime = waitEnd - System.currentTimeMillis();
            if (waitTime <= 0) {
                fail();
            }
            Components.checkAssertions();
            try {
                assertTrue(Components.awaitExhaustion(waitTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

}
