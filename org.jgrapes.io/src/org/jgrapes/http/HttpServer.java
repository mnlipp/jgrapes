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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jdrupes.httpcodec.RequestResult;
import org.jdrupes.httpcodec.EncoderResult;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jgrapes.core.Component;
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
public class HttpServer extends Component {

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

	private Channel networkChannel;
	private Map<Connection, ConnectionAttachments> 
		connectionData = new WeakHashMap<>();
	private List<Class<? extends Request>> providedFallbacks;

	/**
	 * Create a new server that uses the {@code networkChannel} for network
	 * level I/O. 
	 * <P>
	 * As a convenience the server can provide fall back
	 * handlers for the specified types of requests. The fall
	 * back handler simply returns 404 ("Not found").
	 * 
	 * @param componentChannel this component's channel
	 * @param networkChannel the channel for network level I/O
	 * @param fallbacks the requests for which a fall back handler
	 * is provided
	 */
	@SafeVarargs
	public HttpServer(Channel componentChannel, Channel networkChannel,
			Class<? extends Request>... fallbacks) {
		super(componentChannel);
		this.networkChannel = networkChannel;
		this.providedFallbacks = Arrays.asList(fallbacks);
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

	/**
	 * Handles a new client connection. Creates a new downstream connection
	 * as {@link Extension} of the network connection, a 
	 * {@link HttpRequestDecoder} and a {@link HttpResponseEncoder}.
	 * 
	 * @param event the accepted event
	 */
	@DynamicHandler
	public void onAccepted(Accepted<ManagedByteBuffer> event) {
		connectionData.put(event.getConnection(), new ConnectionAttachments(
		        new Extension(this, event.getConnection()),
		        new HttpRequestDecoder(), new HttpResponseEncoder()));
	}

	/**
	 * Handles data from the client. The data is send through the 
	 * {@link HttpRequestDecoder} and events are sent downstream
	 * according to the decoding results.
	 * 
	 * @param event
	 */
	@DynamicHandler
	public void onRead(Read<ManagedByteBuffer> event) {
		final ConnectionAttachments conData = connectionData
		        .get(event.getConnection());
		// Get data associated with the channel
		final HttpRequestDecoder httpDecoder = conData.decoder;
		final DataConnection downConn = conData.downStreamConnection;
		if (httpDecoder == null) {
			throw new IllegalStateException(
			        "Read event for unknown connection.");
		}
	
		// Send the data from the event through the decoder. 
		ByteBuffer buffer = event.getBuffer().getBacking();
		while (buffer.hasRemaining()) {
			RequestResult result = httpDecoder.decode(buffer);
			if (result.hasMessage()) {
				fireRequest(downConn, result.getMessage());
			}
			if (result.hasResponse()) {
				fire(new Response(downConn, result.getResponse()));
				if (result.getCloseConnection()) {
					fire(new Close<>(downConn));
				}
			}
		}
	}

	/**
	 * Creates a specific request event as appropriate for the request
	 * and fires it.
	 * 
	 * @param connection the downstream connection
	 * @param request the decoded request
	 */
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

	/**
	 * Handles a response event from downstream by sending it through an
	 * {@link HttpResponseEncoder} that generates the data (encoded
	 * information) and sends it upstream with {@link Write} events. 
	 * Depending on the response data, subsequent {@link Write} events 
	 * and an {@link Eof} event targeted at the {@link HttpServer} can 
	 * follow.
	 * 
	 * @param event the repsonse event
	 * @throws InterruptedException
	 */
	@Handler
	public void onResponse(Response event) throws InterruptedException {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		final ConnectionAttachments connData = connectionData.get(netConn);
		final HttpResponseEncoder encoder = connData.encoder;
		final HttpResponse response = event.getResponse();

		// Start sending the response
		encoder.encode(response);
		while (true) {
			connData.outBuffer = netConn.acquireByteBuffer();
			final ManagedByteBuffer buffer = connData.outBuffer;
			EncoderResult result = encoder.encode(buffer.getBacking());
			if (result.isOverflow()) {
				(new Write<>(netConn, buffer)).fire();
				continue;
			}
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
	}

	/**
	 * Received the message body. A {@link Response} event that has a
	 * message body can be followed by one or more {@link Write} events
	 * from downstream that contain the data. An {@link Eof} event signals
	 * the end of the message body.
	 * 
	 * @param event the event with the data
	 * @throws InterruptedException
	 */
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

	/**
	 * Signals the end of a message body.
	 * 
	 * @param event the event
	 * @throws InterruptedException
	 */
	@Handler
	public void onEof(Eof event) throws InterruptedException {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		flush(netConn);
	}

	/**
	 * Handles a close event from downstream by flushing any remaining
	 * data and sending a {@link Close} event upstream.
	 * 
	 * @param event the close event
	 * @throws InterruptedException 
	 */
	@Handler
	public void onClose(Close<?> event) throws InterruptedException {
		final DataConnection netConn 
			= ((Extension)event.getConnection()).getUpstreamConnection();
		flush(netConn);
		(new Close<>(netConn)).fire();
	}

	/**
	 * Sends any data still remaining in the out buffer upstream.
	 * 
	 * @param netConn
	 * @throws InterruptedException
	 */
	private void flush(final DataConnection netConn)
	        throws InterruptedException {
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

	/**
	 * Checks whether the request has been handled (status code
	 * of response has been set). If not, send the default 
	 * response ("Not implemented") to the client.
	 * 
	 * @param event the request completed event
	 */
	@Handler
	public void onRequestCompleted(Request.Completed event)
	        throws InterruptedException, ParseException {
		final Request requestEvent = event.getCompleted();
		final HttpResponse response = requestEvent.getRequest().getResponse();
		final DataConnection connection = requestEvent.getConnection();

		if (response.getStatusCode() == HttpStatus.NOT_IMPLEMENTED
		        .getStatusCode()) {
			(new Response(connection, response)).fire();
		}
	}

	/**
	 * Provides a fallback handler for an OPTIONS request with
	 * asterisk. Simply responds with "OK".
	 * 
	 * @param event
	 * @throws ParseException
	 */
	@Handler(priority=Integer.MIN_VALUE)
	public void onOptions(OptionsRequest event) throws ParseException {
		if (event.getRequestUri() == HttpRequest.ASTERISK_REQUEST) {
			HttpResponse response = event.getRequest().getResponse();
			response.setStatus(HttpStatus.OK);
			fire(new Response(event.getConnection(), response));
			event.stop();
		}
	}

	/**
	 * Provides a fallback handler (lowest priority) for the request
	 * types specified in the constructor. 
	 * 
	 * @param event
	 * @throws ParseException
	 */
	@Handler(priority=Integer.MIN_VALUE)
	public void onRequest(Request event) throws ParseException {
		if (providedFallbacks == null 
				|| !providedFallbacks.contains(event.getClass())) {
			return;
		}
		final HttpResponse response = event.getRequest().getResponse();
		final DataConnection connection = event.getConnection();
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
		event.stop();
	}
}
