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
package org.jgrapes.http;

import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jdrupes.httpcodec.DecoderResult;
import org.jdrupes.httpcodec.EncoderResult;
import org.jdrupes.httpcodec.HttpMediaTypeFieldValue;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.DynamicHandler;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.ConnectRequest;
import org.jgrapes.http.events.DeleteRequest;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.HeadRequest;
import org.jgrapes.http.events.OptionsRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.http.events.PutRequest;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.TraceRequest;
import org.jgrapes.http.events.Request.HandlingResult;
import org.jgrapes.io.Connection;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.io.util.ManagedCharBuffer;
import org.jgrapes.net.Server;
import org.jgrapes.net.events.Accepted;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServer extends AbstractComponent {

	private Channel networkChannel;
	private Map<Connection, HttpRequestDecoder> decoders = new WeakHashMap<>();
	private Map<Connection, HttpResponseEncoder> encoders = new WeakHashMap<>();

	/**
	 * Create a new server that uses the {@code serverChannel} for
	 * network level I/O. 
	 * 
	 * @param componentChannel this component's channel
	 * @param networkChannel the channel for network level I/O
	 */
	public HttpServer(Channel componentChannel, Channel networkChannel) {
		super(componentChannel);
		this.networkChannel = networkChannel;
		addHandler("onAccepted", networkChannel.getMatchKey());
		addHandler("onRead", networkChannel.getMatchKey());
	}
	
	/**
	 * Create a new server that creates its own {@link Server} with
	 * the given address and uses it for network level I/O.
	 * 
	 * @param componentChannel
	 */
	public HttpServer(Channel componentChannel, SocketAddress serverAddress) {
		super(componentChannel);
		Server server = new Server(Channel.SELF, serverAddress);
		networkChannel = server;
		attach(server);
		addHandler("onAccepted", networkChannel.getMatchKey());
		addHandler("onRead", networkChannel.getMatchKey());
	}

	@DynamicHandler
	public void onAccepted(Accepted<ManagedByteBuffer> event) {
		decoders.put(event.getConnection(), new HttpRequestDecoder());
		encoders.put(event.getConnection(), new HttpResponseEncoder());
	}
	
	@DynamicHandler
	public void onRead(Read<ManagedByteBuffer> event) {
		HttpRequestDecoder httpDecoder = decoders.get(event.getConnection());
		if (httpDecoder == null) {
			throw new IllegalStateException(
			        "Read event for unknown connection.");
		}
		ByteBuffer buffer = event.getBuffer().getBuffer();
		while (buffer.hasRemaining()) {
			DecoderResult result = httpDecoder.decode(buffer);
			if (result.hasRequest()) {
				fireRequest(event.getConnection(), result.getRequest());
			}
			if (result.hasResponse()) {
				fire (new Response(event.getConnection(), 
						result.getResponse()));
			}
			if (result.mustBeClosed()) {
				fire (new Close<>(event.getConnection()), networkChannel);
			}
		}
	}

	private void fireRequest(DataConnection connection,
			HttpRequest request) {
		Request req;
		switch (request.getMethod()) {
		case "OPTIONS":
			req = new OptionsRequest(connection, request);
			break;
		case "GET":
			req = new GetRequest(connection, request);
			break;
		case "HEAD":
			req = new HeadRequest(connection, request);
			break;
		case "POST":
			req = new PostRequest(connection, request);
			break;
		case "PUT":
			req = new PutRequest(connection, request);
			break;
		case "DELETE":
			req = new DeleteRequest(connection, request);
			break;
		case "TRACE":
			req = new TraceRequest(connection, request);
			break;
		case "CONNECT":
			req = new ConnectRequest(connection, request);
			break;
		default:
			req = new Request(connection, request);
			break;
		}
		fire(req);
	}

	@Handler
	public void onRequestCompleted(Request.Completed event) 
			throws InterruptedException, ParseException {
		Request requestEvent = event.getCompleted();
		DataConnection connection = requestEvent.getConnection();

		HttpResponse response = null;
		switch (requestEvent.get()) {
		case UNHANDLED:
			response = new HttpResponse
				(requestEvent.getRequest().getProtocol(), 
						HttpStatus.NOT_IMPLEMENTED, false);
			fire (new Response(connection, response));
			break;
		case RESOURCE_NOT_FOUND:
			response = new HttpResponse(requestEvent.getRequest().getProtocol(),
					HttpStatus.NOT_FOUND, true);
			response.setContentType("text", "plain");
			fire (new Response(connection, response));
			fire (Write.wrap(connection, "Not Found"));
			fire (new Eof(connection));
			break;
		default:
			break;
		}
	}
	
	@Handler
	public void onResponse(Response event) throws InterruptedException {
		DataConnection connection = event.getConnection();
		HttpResponse response = event.getResponse();
		HttpResponseEncoder encoder = encoders.get(connection);
		EventPipeline pipeline = newEventPipeline();
		
		// Send response
		while (true) {
			ManagedByteBuffer buffer = connection.acquireByteBuffer();
			EncoderResult result = encoder.encode(response, buffer.getBuffer());
			pipeline.add(new Write<>(connection, buffer), networkChannel);
			if (!result.isOverflow()) {
				break;
			}
		}
	}
	
	@Handler
	public void onWrite(Write<ManagedBuffer<?>> event) {
		Buffer buffer = event.getBuffer().getBuffer();
		if (buffer instanceof CharBuffer) {
			
		}
	}

	@Handler
	public void onOptions(OptionsRequest event) {
		if (event.getRequestUri() == HttpRequest.ASTERISK_REQUEST) {
			event.setResult(HandlingResult.RESPONDED);
		}
	}
}
