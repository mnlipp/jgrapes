/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2018  Michael N. Lipp
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import net.jodah.concurrentunit.Waiter;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.PurgeTerminator;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SslCodec;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Ready;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServerTest {

    private static TestServer server;

    int minTime = 0;
    int maxTime = 0;
    long accumTime = 0;
    int calls = 0;

    public static class TestServer extends Component {

        private InetSocketAddress addr;
        private WaitForTests<Ready> readyMonitor;

        public TestServer() throws IOException, KeyStoreException,
                NoSuchAlgorithmException, CertificateException,
                UnrecoverableKeyException, KeyManagementException {
            super();
            // Attach a general nio dispatcher
            attach(new NioDispatcher());

            // Create TLS "converter"
            KeyStore serverStore = KeyStore.getInstance("JKS");
            try (FileInputStream kf
                = new FileInputStream("test-resources/localhost.jks")) {
                serverStore.load(kf, "nopass".toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(serverStore, "nopass".toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            // Network level unencrypted channel.
            Channel httpTransport = new NamedChannel("httpTransport");

            // Create a TCP server for SSL
            TcpServer securedNetwork = attach(new TcpServer()
                .setBacklog(3000)
                .setConnectionLimiter(new PermitsPool(50)));
            attach(new SslCodec(httpTransport, securedNetwork, sslContext));

            // Create an HTTP server as converter between transport and
            // application
            // layer.
            attach(new HttpServer(channel(),
                httpTransport, Request.In.Get.class, Request.In.Post.class)
                    .setAcceptNoSni(true));

            // Build application layer
            attach(new FileStorage(channel(), 65536));
            attach(new StaticContentDispatcher(channel(),
                "/**", Paths.get("test-resources/static-content").toUri()));
            attach(new PurgeTerminator(channel()));

            readyMonitor = new WaitForTests<>(this, Ready.class,
                securedNetwork.channel().defaultCriterion());
        }

        public InetSocketAddress getSocketAddress()
                throws InterruptedException, ExecutionException {
            if (addr == null) {
                Ready readyEvent = (Ready) readyMonitor.get();
                if (!(readyEvent
                    .listenAddress() instanceof InetSocketAddress)) {
                    fail();
                }
                addr = ((InetSocketAddress) readyEvent.listenAddress());
            }
            return addr;
        }

        public int getPort()
                throws InterruptedException, ExecutionException {
            return getSocketAddress().getPort();
        }
    }

    @BeforeClass
    public static void startServer() throws IOException, InterruptedException,
            ExecutionException, UnrecoverableKeyException,
            KeyManagementException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException {
        server = new TestServer();
        Components.start(server);

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

    }

    @AfterClass
    public static void stopServer() throws InterruptedException {
        server.fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
        Components.checkAssertions();
    }

    @Test
    public void testConcurrentGetRoot()
            throws IOException, InterruptedException, ExecutionException,
            TimeoutException {
        Waiter waiter = new Waiter();

        URL url = new URL("https", "localhost", server.getPort(), "/");

        int threadCount = 1000;
        if (Boolean.parseBoolean(
            System.getenv().getOrDefault("TRAVIS", "false"))) {
            threadCount = 100;
        }

        final List<Thread> threads = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(0);
        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            Thread getThread = new Thread() {
                @Override
                public void run() {
                    try {
                        synchronized (threads) {
                            try {
                                pending.incrementAndGet();
                                threads.wait();
                            } catch (InterruptedException e) {
                                // ignored
                            }
                        }
                        setName("Getter " + threadNumber);
                        long start = System.currentTimeMillis();
                        HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                        String content;
                        try (BufferedReader buffer = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                            content = buffer.lines()
                                .collect(Collectors.joining("\n"));
                        }
                        long stop = System.currentTimeMillis();
                        int used = (int) (stop - start);
                        synchronized (ServerTest.this) {
                            if (calls == 0) {
                                minTime = used;
                                maxTime = used;
                            } else {
                                minTime = Math.min(minTime, used);
                                maxTime = Math.max(maxTime, used);
                            }
                            calls += 1;
                            accumTime += used;
                        }
                        waiter.assertEquals(200, conn.getResponseCode());
                        waiter.assertTrue(content.contains("Demo"));
                    } catch (IOException e) {
                        waiter.fail(e);
                    } finally {
                        waiter.resume();
                    }
                }
            };
            threads.add(getThread);
            getThread.start();
        }
        while (pending.get() < threads.size()) {
            Thread.sleep(100);
        }
        Thread.sleep(100);
        synchronized (threads) {
            threads.notifyAll();
        }
        waiter.await(60000, threads.size());

        // Cleanup.
        while (threads.size() > 0) {
            threads.remove(0).join();
        }

        System.out.println("min/avg/max (ms): "
            + minTime + "/" + (accumTime / calls) + "/" + maxTime);
    }

//    @Test
//    public void testHttps() throws IOException, InterruptedException,
//            ExecutionException, TimeoutException, KeyStoreException,
//            NoSuchAlgorithmException, CertificateException,
//            UnrecoverableKeyException, KeyManagementException {
//        // Create client
//        ClientApp clntApp = new ClientApp(server.getSocketAddress());
//        clntApp.attach(new NioDispatcher());
//
//        // Create a TCP connector for SSL
//        TcpConnector secClntNetwork = clntApp.attach(new TcpConnector());
//        clntApp.attach(new SslCodec(clntApp, secClntNetwork, true));
//        WaitForTests done
//            = new WaitForTests(clntApp, Done.class, clntApp.defaultCriterion());
//        Components.start(clntApp);
//        done.get();
//
//        // Stop
//        Components.manager(clntApp).fire(new Stop(), Channel.BROADCAST);
//        long waitEnd = System.currentTimeMillis() + 300000;
//        while (true) {
//            long waitTime = waitEnd - System.currentTimeMillis();
//            if (waitTime <= 0) {
//                fail();
//            }
//            Components.checkAssertions();
//            try {
//                assertTrue(Components.awaitExhaustion(waitTime));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            break;
//        }
//        Components.checkAssertions();
//    }

}
