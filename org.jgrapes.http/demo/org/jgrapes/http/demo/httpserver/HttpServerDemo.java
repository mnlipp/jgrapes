/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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

package org.jgrapes.http.demo.httpserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.net.SslServer;
import org.jgrapes.net.TcpServer;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServerDemo extends Component {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws UnrecoverableKeyException 
	 * @throws CertificateException 
	 * @throws KeyManagementException 
	 */
	public static void main(String[] args) 
			throws IOException, InterruptedException, 
			NoSuchAlgorithmException, KeyStoreException, 
			UnrecoverableKeyException, CertificateException, 
			KeyManagementException {
		// The demo component is the application
		HttpServerDemo app = new HttpServerDemo();
		// Attach a general nio dispatcher
		app.attach(new NioDispatcher());

		// Network level unencrypted channel.
		Channel httpTransport = new NamedChannel("httpTransport");
		// Create a TCP server listening on port 8888
		app.attach(new TcpServer(httpTransport, new InetSocketAddress(8888)));

		// Create TLS "converter"
		KeyStore serverStore = KeyStore.getInstance("JKS");
		try (FileInputStream kf 
				= new FileInputStream("demo-resources/localhost.jks")) {
			serverStore.load(kf, "nopass".toCharArray());
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(
				KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverStore, "nopass".toCharArray());
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
		// Create a TCP server for SSL
		Channel securedNetwork = app.attach(
				new TcpServer(SELF, new InetSocketAddress(4443)));
		app.attach(new SslServer(httpTransport, securedNetwork, sslContext));

		// Create an HTTP server as converter between transport and application
		// layer.
		app.attach(new HttpServer(app.channel(), 
		        httpTransport, GetRequest.class, PostRequest.class));
		
		// Build application layer
		app.attach(new FileStorage(app.channel(), 65536));
		app.attach(new StaticContentDispatcher(app.channel(),
		        "/**", Paths.get("demo-resources/static-content")));
		app.attach(new StaticContentDispatcher(app.channel(),
		        "/doc|**", Paths.get("../../jgrapes.gh-pages/javadoc")));
		Components.start(app);
	}

}
