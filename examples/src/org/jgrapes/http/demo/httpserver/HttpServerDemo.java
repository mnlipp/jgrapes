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

package org.jgrapes.http.demo.httpserver;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.events.Stop;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.InMemorySessionManager;
import org.jgrapes.http.LanguageSelector;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SslCodec;
import org.jgrapes.net.SocketServer;
import org.jgrapes.util.PreferencesStore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 */
public class HttpServerDemo extends Component implements BundleActivator {

    private HttpServerDemo app;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        // The demo component is the application
        app = new HttpServerDemo();
        // Add preferences store
        app.attach(new PreferencesStore(app.channel(), HttpServerDemo.class));
        // Attach a general nio dispatcher
        app.attach(new NioDispatcher());

        // Network level unencrypted channel.
        Channel httpTransport = new NamedChannel("httpTransport");
        // Create a TCP server listening on port 8888
        // (may be overridden by preferences)
        app.attach(new SocketServer(httpTransport)
            .setServerAddress(new InetSocketAddress(8888))
            .setName("HttpServer"));

        // Create TLS "converter"
        KeyStore serverStore = KeyStore.getInstance("JKS");
        try (InputStream keyFile
            = Files.newInputStream(Paths.get("demo-resources/localhost.jks"))) {
            serverStore.load(keyFile, "nopass".toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverStore, "nopass".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
        // Create a TCP server for SSL
        Channel securedNetwork = app.attach(
            new SocketServer().setServerAddress(new InetSocketAddress(4443))
                .setBacklog(3000).setConnectionLimiter(new PermitsPool(50))
                .setName("HttpsServer"));
        app.attach(new SslCodec(httpTransport, securedNetwork, sslContext));

        // Create an HTTP server as converter between transport and application
        // layer.
        app.attach(new HttpServer(app,
            httpTransport, Request.In.Get.class, Request.In.Post.class));

        // Build application layer
        app.attach(new InMemorySessionManager(app.channel()));
        app.attach(new LanguageSelector(app.channel()));
        app.attach(new FileStorage(app.channel(), 65536));
        app.attach(new StaticContentDispatcher(app.channel(),
            "/**", Paths.get("demo-resources/static-content").toUri()));
        app.attach(new StaticContentDispatcher(app.channel(),
            "/doc|**", Paths.get("../../jgrapes.gh-pages/javadoc").toUri()));
        app.attach(new FormProcessor(app.channel()));
        app.attach(new WsEchoServer(app.channel()));
        Components.start(app);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        app.fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion();
    }

    /**
     * @param args
     * @throws Exception 
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public static void main(String[] args) throws Exception {
        new HttpServerDemo().start(null);
    }

}
