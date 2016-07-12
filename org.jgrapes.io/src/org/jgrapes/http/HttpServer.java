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

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jdrupes.httpcodec.DecoderResult;
import org.jdrupes.httpcodec.EncoderResult;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jgrapes.core.AbstractComponent;
import org.jgrapes.core.Channel;
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
import org.jgrapes.io.Extension;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Eof;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.Server;
import org.jgrapes.net.events.Accepted;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServer extends AbstractComponent {

	private Channel networkChannel;

	private class ConnectionAttachments {
		public Extension downStreamConnection;
		public HttpRequestDecoder decoder;
		public HttpResponseEncoder encoder;
		public ManagedByteBuffer outBuffer;

		public ConnectionAttachments(Extension downStreamChannel,
		        HttpRequestDecoder decoder, HttpResponseEncoder encoder) {
			super();
			this.downStreamConnection = downStreamChannel;
			this.decoder = decoder;
			this.encoder = encoder;
		}
	}

	private Map<Connection, ConnectionAttachments> connectionData = new WeakHashMap<>();

	/**
	 * Create a new server that uses the {@code networkChannel} for network
	 * level I/O.
	 * 
	 * @param componentChannel
	 *            this component's channel
	 * @param networkChannel
	 *            the channel for network level I/O
	 */
	public HttpServer(Channel componentChannel, Channel networkChannel) {
		super(componentChannel);
		this.networkChannel = networkChannel;
		addHandler("onAccepted", networkChannel.getMatchKey());
		addHandler("onRead", networkChannel.getMatchKey());
	}

	/**
	 * Create a new server that creates its own {@link Server} with the given
	 * address and uses it for network level I/O.
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
		connectionData.put(event.getConnection(), new ConnectionAttachments(
		        new Extension(this, event.getConnection()),
		        new HttpRequestDecoder(), new HttpResponseEncoder()));
	}

	@DynamicHandler
	public void onRead(Read<ManagedByteBuffer> event) {
		final ConnectionAttachments conData = connectionData
		        .get(event.getConnection());
		final HttpRequestDecoder httpDecoder = conData.decoder;
		final DataConnection downConn = conData.downStreamConnection;
		if (httpDecoder == null) {
			throw new IllegalStateException(
			        "Read event for unknown connection.");
		}
		ByteBuffer buffer = event.getBuffer().getBacking();
		while (buffer.hasRemaining()) {
			DecoderResult result = httpDecoder.decode(buffer);
			if (result.hasRequest()) {
				fireRequest(downConn, result.getRequest());
			}
			if (result.hasResponse()) {
				fire(new Response(downConn, result.getResponse()));
				if (result.getCloseConnection()) {
					fire(new Close<>(downConn));
				}
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
	public void onResponse(Response event) throws InterruptedException {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		final ConnectionAttachments connData = connectionData.get(netConn);
		final HttpResponseEncoder encoder = connData.encoder;
		final HttpResponse response = event.getResponse();

		// Send response
		encoder.encode(response);
		while (true) {
			connData.outBuffer = netConn.acquireByteBuffer();
			final ManagedByteBuffer buffer = connData.outBuffer;
			EncoderResult result = encoder.encode(buffer.getBacking());
			if (!result.isOverflow()) {
				if (!response.hasBody()) {
					if (buffer.position() > 0) {
						(new Write<>(netConn, buffer)).fire();
					} else {
						buffer.unlockBuffer();
					}
					connData.outBuffer = null;
				}
				break;
			}
			(new Write<>(netConn, buffer)).fire();
		}
	}

	@Handler
	public void onWrite(Write<ManagedBuffer<?>> event)
	        throws InterruptedException {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		final ConnectionAttachments connData = connectionData.get(netConn);
		final HttpResponseEncoder encoder = connData.encoder;

		Buffer in = event.getBuffer().getBacking();
		while (true) {
			EncoderResult result = null;
			if (in instanceof ByteBuffer) {
				result = encoder.encode((ByteBuffer) in,
				        connData.outBuffer.getBacking());
			}
			if (!result.isOverflow()) {
				break;
			}
			(new Write<>(netConn, connData.outBuffer)).fire();
			connData.outBuffer = netConn.acquireByteBuffer();
		}
	}

	@Handler
	public void onEof(Eof event) throws InterruptedException {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		final ConnectionAttachments connData = connectionData.get(netConn);
		final HttpResponseEncoder encoder = connData.encoder;

		// Send remaining data
		while (true) {
			final ManagedByteBuffer buffer = connData.outBuffer;
			EncoderResult result = encoder.encode(null, buffer.getBacking());
			if (!result.isOverflow()) {
				if (buffer.position() > 0) {
					(new Write<>(netConn, buffer)).fire();
				} else {
					buffer.unlockBuffer();
				}
				connData.outBuffer = null;
				break;
			}
			(new Write<>(netConn, buffer)).fire();
			connData.outBuffer = netConn.acquireByteBuffer();
		}
	}

	@Handler
	public void onClose(Close<?> event) {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		final ConnectionAttachments connData = connectionData.get(netConn);
		if (connData.outBuffer != null) {
			connData.outBuffer.unlockBuffer();
			connData.outBuffer = null;
		}
		(new Close<>(netConn)).fire();
	}

	@Handler
	public void onRequestCompleted(Request.Completed event)
	        throws InterruptedException, ParseException {
		final Request requestEvent = event.getCompleted();
		final HttpResponse response = requestEvent.getRequest().getResponse();
		final DataConnection connection = requestEvent.getConnection();

		switch (requestEvent.get()) {
		case UNHANDLED:
			response.setStatus(HttpStatus.NOT_IMPLEMENTED);
			(new Response(connection, response)).fire();
			break;
		case RESOURCE_NOT_FOUND:
			response.setStatus(HttpStatus.NOT_FOUND);
			response.setHasBody(true);
			HttpMediaTypeField media = new HttpMediaTypeField(
			        HttpField.CONTENT_TYPE, "text", "plain");
			media.setParameter("charset", "utf-8");
			response.setHeader(media);
			(new Response(connection, response)).fire();
			try {
				Write.wrap(connection,
				        "Not Found".getBytes("utf-8")).fire();
			} catch (UnsupportedEncodingException e) {
			}
			(new Eof(connection)).fire();
			break;
		default:
			break;
		}
	}

	@Handler
	public void onOptions(OptionsRequest event) throws ParseException {
		if (event.getRequestUri() == HttpRequest.ASTERISK_REQUEST) {
			event.setResult(HandlingResult.RESPONDED);
			HttpResponse response = event.getRequest().getResponse();
			response.setStatus(HttpStatus.OK);
			fire(new Response(event.getConnection(), response));
		}
	}
}
