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

import java.awt.event.InputEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.util.FormUrlDecoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.StaticContentDispatcher;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.EndOfRequest;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.io.FileStorage;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.io.util.PermitsPool;
import org.jgrapes.net.SslServer;
import org.jgrapes.net.TcpServer;

/**
 *
 */
public class HttpServerDemo extends Component {

	private class FormContext {
		public FormUrlDecoder fieldDecoder = new FormUrlDecoder();
	}
	
	private Map<IOSubchannel, FormContext> formContexts 
		= Collections.synchronizedMap(new WeakHashMap<>());
	
	@RequestHandler(patterns="/form")
	public void onPost(PostRequest request, IOSubchannel channel) {
		HttpResponse response = request.request().response().get();
		formContexts.put(channel, new FormContext());
	}
	
	@Handler
	public void onInput(Input<ManagedByteBuffer> event, IOSubchannel channel) {
		FormContext ctx = formContexts.get(channel);
		ctx.fieldDecoder.addData(event.buffer().backingBuffer());

//		while (true) {
//		out.clear();
//		Decoder.Result<?> decoderResult = null;
//		try {
//			decoderResult = engine.decode(in, out, false);
//		} catch (ProtocolException e) {
//			return;
//		}
//		out.flip();
//		fieldDecoder.addData(out);
//		if (decoderResult.isOverflow()) {
//			continue;
//		}
//		if (decoderResult.isUnderflow()) {
//			in.clear();
//			channel.read(in);
//			in.flip();
//			continue;
//		}
//		break;
//	}
//	response.setStatus(HttpStatus.OK);
//	response.setMessageHasBody(true);
//	response.setField(HttpField.CONTENT_TYPE,
//			MediaType.builder().setType("text", "plain")
//			.setParameter("charset", "utf-8").build());
//	String data = "First name: " + fieldDecoder.fields().get("firstname")
//	        + "\r\n" + "Last name: "
//	        + fieldDecoder.fields().get("lastname");
//	ByteBuffer body = ByteBuffer.wrap(data.getBytes("utf-8"));
//	sendResponse(response, body, true);
	}

	@Handler
	public void onEndOfRequest(EndOfRequest event) {
		for (IOSubchannel channel: event.channels(IOSubchannel.class)) {
			FormContext ctx = formContexts.get(channel);
			int i =0;
		}
	}
	
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
		app.attach(new TcpServer(httpTransport)
				.setServerAddress(new InetSocketAddress(8888)));

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
				new TcpServer().setServerAddress(new InetSocketAddress(4443))
				.setBacklog(3000).setConnectionLimiter(new PermitsPool(50)));
		app.attach(new SslServer(httpTransport, securedNetwork, sslContext));

		// Create an HTTP server as converter between transport and application
		// layer.
		app.attach(new HttpServer(app, 
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
