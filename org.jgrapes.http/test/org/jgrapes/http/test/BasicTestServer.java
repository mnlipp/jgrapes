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

package org.jgrapes.http.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SocketServer;
import org.jgrapes.net.SslCodec;
import org.jgrapes.net.events.Ready;
import static org.junit.Assert.fail;

/**
 *
 */
public class BasicTestServer extends Component {
    private InetSocketAddress unsecureAddr;
    private InetSocketAddress secureAddr;
    private WaitForTests<Ready> unsecureMonitor;
    private WaitForTests<Ready> secureMonitor;

    @SafeVarargs
    public BasicTestServer(Class<? extends Request.In>... fallbacks)
            throws IOException, InterruptedException, ExecutionException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        attach(new NioDispatcher());

        // Network level unencrypted channel.
        Channel plainChannel = new NamedChannel("plainTransport");

        // Create a TCP server
        SocketServer unsecureNetwork = attach(new SocketServer(plainChannel));
        unsecureMonitor = new WaitForTests<>(this, Ready.class,
            unsecureNetwork.channel().defaultCriterion());

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

        // Create a TCP server for SSL
        SocketServer securedNetwork
            = attach(new SocketServer(new NamedChannel("tlsTransport"))
                .setBacklog(3000).setConnectionLimiter(new PermitsPool(50)));
        attach(new SslCodec(unsecureNetwork.channel(), securedNetwork.channel(),
            sslContext));

        // Create an HTTP server as converter between transport and
        // application layer.
        attach(new HttpServer(channel(),
            plainChannel, fallbacks).setAcceptNoSni(true));

        secureMonitor = new WaitForTests<>(this, Ready.class,
            securedNetwork.channel().defaultCriterion());
    }

    public InetSocketAddress getUnsecureAddress()
            throws InterruptedException, ExecutionException {
        if (unsecureAddr == null) {
            Ready readyEvent = (Ready) unsecureMonitor.get();
            if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
                fail();
            }
            unsecureAddr = ((InetSocketAddress) readyEvent.listenAddress());
        }
        return unsecureAddr;

    }

    public InetSocketAddress getSecureAddress()
            throws InterruptedException, ExecutionException {
        if (secureAddr == null) {
            Ready readyEvent = (Ready) secureMonitor.get();
            if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
                fail();
            }
            secureAddr = ((InetSocketAddress) readyEvent.listenAddress());
        }
        return secureAddr;

    }

    public int getPort()
            throws InterruptedException, ExecutionException {
        return getUnsecureAddress().getPort();
    }

    public int getTlsPort()
            throws InterruptedException, ExecutionException {
        return getSecureAddress().getPort();
    }
}
