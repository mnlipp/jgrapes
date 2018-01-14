package org.jgrapes.http.test;

import java.io.FileInputStream;
import java.io.IOException;
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
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SslServer;
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
		private WaitForTests readyMonitor;

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
			attach(new SslServer(httpTransport, securedNetwork, sslContext));

			// Create an HTTP server as converter between transport and application
			// layer.
			attach(new HttpServer(channel(), 
			        httpTransport, GetRequest.class, PostRequest.class)
					.setAcceptNoSni(true));
			
			// Build application layer
			attach(new FileStorage(channel(), 65536));
			attach(new StaticContentDispatcher(channel(),
					"/**", Paths.get("test-resources/static-content").toUri()));
			
			readyMonitor = new WaitForTests(this, Ready.class, 
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
		AtomicInteger waiting = new AtomicInteger(0);
		for (int i = 0; i < threadCount; i++) {
			Thread getThread = new Thread() {
				@Override
				public void run() {
					try {
						synchronized (threads) {
							try {
								waiting.incrementAndGet();
								threads.wait();
							} catch (InterruptedException e) {
								// ignored
							}
						}
						long start = System.currentTimeMillis();
						HttpURLConnection conn = (HttpURLConnection) url
						        .openConnection();
						conn.getInputStream().close();
						long stop = System.currentTimeMillis();
						int used = (int)(stop - start);
						synchronized (ServerTest.this) {
							if (calls == 0) {
								minTime = used;
								maxTime = used;
							} else {
								minTime = Math.min(minTime,  used);
								maxTime = Math.max(maxTime, used);
							}
							calls += 1;
							accumTime += used;
						}
						waiter.assertEquals(200, conn.getResponseCode());
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
		while (waiting.get() < threads.size()) {
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
	
}
