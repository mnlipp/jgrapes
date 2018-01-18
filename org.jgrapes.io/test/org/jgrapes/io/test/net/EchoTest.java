/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io.test.net;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.test.WaitForTests;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.net.SslServer;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Ready;

import static org.junit.Assert.*;
import org.junit.Test;

public class EchoTest {

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
			out.backingBuffer().put(event.backingBuffer());
			channel.respond(Output.fromSink(out, event.isEndOfRecord()));
		}
	}

	private class Receiver implements Callable<Integer> {

		private InputStream fromServer;
		private int times;

		/**
		 * @param fromServer
		 */
		public Receiver(InputStream fromServer, int times) {
			this.fromServer = fromServer;
			this.times = times;
		}

		@Override
		public Integer call() throws Exception {
			final AtomicInteger expected = new AtomicInteger(0);
			BufferedReader in = new BufferedReader(
						new InputStreamReader(fromServer, "ascii"));
			while (expected.get() < times) {
				String line = in.readLine();
				if (line == null) {
					break;
				}
				String[] parts = line.split(":");
				assertEquals(expected.get(), Integer.parseInt(parts[0]));
				assertEquals("Hello World!", parts[1]);
				expected.incrementAndGet();
			}
			return expected.get();
		}
	}

	private class Sender extends Thread {

		private OutputStream toServer;
		private int times;
		
		public Sender(OutputStream toServer, int times) {
			this.toServer = toServer;
			this.times = times;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < times; i++) {
					String line = i + ":Hello World!\n";
					toServer.write(line.getBytes("ascii"));
//					if (times < 100) {
						toServer.flush();
						try {
							// If we're too fast, data will be appended
							// to previous, not yet flushed out chunk
							Thread.sleep(5);
						} catch (InterruptedException e) {
							// Ignored
						}
//					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testTcp() throws IOException, InterruptedException, 
			ExecutionException, TimeoutException {
		EchoServer app = new EchoServer();
		app.attach(new TcpServer(app));
		app.attach(new NioDispatcher());
		WaitForTests wf = new WaitForTests(
				app, Ready.class, app.defaultCriterion());
		Components.start(app);
		Ready readyEvent = (Ready) wf.get();
		if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
			fail();
		}
		InetSocketAddress serverAddr = new InetSocketAddress("localhost",
				((InetSocketAddress)readyEvent.listenAddress()).getPort());
		try (Socket client = new Socket(serverAddr.getAddress(),
		        serverAddr.getPort())) {
			FutureTask<Integer> receive = new FutureTask<>(
					new Receiver(client.getInputStream(), 16));
			new Thread(receive).start();

			new Sender(client.getOutputStream(), 16).run();

			int received = receive.get(5000, TimeUnit.MILLISECONDS);
			assertEquals(16, received);
		}
	
		Components.manager(app).fire(new Stop(), Channel.BROADCAST);
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
		Components.checkAssertions();
	}

	@Test
	public void testSsl() throws IOException, InterruptedException, 
			ExecutionException, TimeoutException, KeyStoreException, 
			NoSuchAlgorithmException, CertificateException, 
			UnrecoverableKeyException, KeyManagementException {
		EchoServer app = new EchoServer();
		app.attach(new NioDispatcher());
		
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
		TcpServer securedNetwork = app.attach(new TcpServer());
		app.attach(new SslServer(app, securedNetwork, sslContext));
		
		// App ready.
		WaitForTests wf = new WaitForTests(
				securedNetwork, Ready.class, securedNetwork.defaultCriterion());
		Components.start(app);
		Ready readyEvent = (Ready) wf.get();
		if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
			fail();
		}
		InetSocketAddress serverAddr = new InetSocketAddress("localhost",
				((InetSocketAddress)readyEvent.listenAddress()).getPort());
		
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{
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
		SSLContext clientContext = SSLContext.getInstance("SSL");
	    clientContext.init(null, trustAllCerts, null);
		SSLSocketFactory sslsocketfactory = clientContext.getSocketFactory();
		try (SSLSocket client = (SSLSocket) sslsocketfactory.createSocket(
				serverAddr.getAddress(), serverAddr.getPort())) {
			client.startHandshake();
			int expected = 256;
			FutureTask<Integer> receive = new FutureTask<>(
					new Receiver(client.getInputStream(), expected));
			new Thread(receive).start();

			new Sender(client.getOutputStream(), expected).run();

			int received = receive.get(5000, TimeUnit.MILLISECONDS);
			assertEquals(expected, received);
		}
	
		Components.manager(app).fire(new Stop(), Channel.BROADCAST);
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
		Components.checkAssertions();
	}

}
