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

import org.jdrupes.httpcodec.HttpRequest;
import org.jdrupes.httpcodec.HttpResponse;
import org.jdrupes.httpcodec.HttpCodec;
import org.jdrupes.httpcodec.HttpCodec.HttpStatus;
import org.jdrupes.httpcodec.fields.HttpField;
import org.jdrupes.httpcodec.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.HttpRequestDecoder;
import org.jdrupes.httpcodec.HttpResponseEncoder;
import org.jgrapes.core.Component;
import org.jgrapes.core.Channel;
import org.jgrapes.core.annotation.DynamicHandler;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.ConnectRequest;
import org.jgrapes.http.events.DeleteRequest;
import org.jgrapes.http.events.EndOfRequest;
import org.jgrapes.http.events.EndOfResponse;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.HeadRequest;
import org.jgrapes.http.events.OptionsRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.http.events.PutRequest;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.TraceRequest;
import org.jgrapes.io.DataConnection;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Read;
import org.jgrapes.io.events.Write;
import org.jgrapes.io.util.BufferCollector;
import org.jgrapes.io.util.Extension;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.Server;
import org.jgrapes.net.events.Accepted;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServer extends Component {

	private class ExtExtension extends Extension {
		public HttpRequestDecoder decoder;
		public HttpResponseEncoder encoder;
		public ManagedByteBuffer outBuffer;

		public ExtExtension(DataConnection upstreamChannel) {
			super(HttpServer.this, upstreamChannel);
			this.decoder = new HttpRequestDecoder();
			this.encoder = new HttpResponseEncoder();
		}
	}

	private Channel networkChannel;
	private List<Class<? extends Request>> providedFallbacks;

	/**
	 * Create a new server that uses the {@code networkChannel} for network
	 * level I/O.
	 * <P>
	 * As a convenience the server can provide fall back handlers for the
	 * specified types of requests. The fall back handler simply returns 404 (
	 * "Not found").
	 * 
	 * @param componentChannel
	 *            this component's channel
	 * @param networkChannel
	 *            the channel for network level I/O
	 * @param fallbacks
	 *            the requests for which a fall back handler is provided
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
	@SafeVarargs
	public HttpServer(Channel componentChannel, SocketAddress serverAddress,
	        Class<? extends Request>... fallbacks) {
		super(componentChannel);
		this.providedFallbacks = Arrays.asList(fallbacks);
		Server server = new Server(Channel.SELF, serverAddress);
		networkChannel = server;
		attach(server);
		addHandler("onAccepted", networkChannel.getMatchKey());
		addHandler("onRead", networkChannel.getMatchKey());
	}

	/**
	 * Creates a new downstream connection as {@link Extension} of the network
	 * connection, a {@link HttpRequestDecoder} and a
	 * {@link HttpResponseEncoder}.
	 * 
	 * @param event
	 *            the accepted event
	 */
	@DynamicHandler
	public void onAccepted(Accepted<ManagedByteBuffer> event) {
		new ExtExtension(event.getConnection());
	}

	/**
	 * Handles data from the client. The data is send through the
	 * {@link HttpRequestDecoder} and events are sent downstream according to
	 * the decoding results.
	 * 
	 * @param event
	 */
	@DynamicHandler
	public void onRead(Read<ManagedByteBuffer> event) {
		final ExtExtension extDown = (ExtExtension) Extension
		        .lookupExtension(event.getConnection());
		// Get data associated with the channel
		final HttpRequestDecoder httpDecoder = extDown.decoder;
		if (httpDecoder == null) {
			throw new IllegalStateException(
			        "Read event for unknown connection.");
		}

		// Send the data from the event through the decoder.
		ByteBuffer in = event.getBuffer().getBacking();
		ManagedByteBuffer bodyData = null;
		while (in.hasRemaining()) {
			HttpRequestDecoder.Result result = httpDecoder.decode(in,
			        bodyData == null ? null : bodyData.getBacking(), false);
			if (result.isHeaderCompleted()) {
				fireRequest(extDown, httpDecoder.getHeader());
			}
			if (result.hasResponse()) {
				// Error during decoding, send back
				fire(new Response(extDown, result.getResponse()));
				break;
			}
			if (bodyData != null && bodyData.position() > 0) {
				fire(new Read<>(extDown, bodyData));
			}
			if (result.isOverflow()) {
				bodyData = new ManagedByteBuffer(
				        ByteBuffer.allocate(in.capacity()),
				        BufferCollector.NOOP_COLLECTOR);
				continue;
			}
			if (!result.isUnderflow()
			        && httpDecoder.getHeader().messageHasBody()) {
				fire(new EndOfRequest(extDown));
			}
		}
	}

	/**
	 * Creates a specific request event as appropriate for the request and fires
	 * it.
	 * 
	 * @param connection
	 *            the downstream connection
	 * @param request
	 *            the decoded request
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
	 * {@link HttpResponseEncoder} that generates the data (encoded information)
	 * and sends it upstream with {@link Write} events. Depending on the
	 * response data, subsequent {@link Write} events and an
	 * {@link EndOfResponse} event targeted at the {@link HttpServer} can
	 * follow.
	 * 
	 * @param event
	 *            the repsonse event
	 * @throws InterruptedException
	 */
	@Handler
	public void onResponse(Response event) throws InterruptedException {
		if (!(event.getConnection() instanceof ExtExtension)) {
			return;
		}
		final ExtExtension extDown = (ExtExtension)event.getConnection();
		final DataConnection netConn = extDown.getUpstreamConnection();
		final HttpResponseEncoder encoder = extDown.encoder;
		final HttpResponse response = event.getResponse();

		// Start sending the response
		encoder.encode(response);
		while (true) {
			extDown.outBuffer = netConn.acquireByteBuffer();
			final ManagedByteBuffer buffer = extDown.outBuffer;
			HttpResponseEncoder.Result result = encoder
			        .encode(HttpCodec.EMPTY_IN, buffer.getBacking(), false);
			if (result.isOverflow()) {
				(new Write<>(netConn, buffer)).fire();
				continue;
			}
			if (!response.messageHasBody()) {
				if (buffer.position() > 0) {
					(new Write<>(netConn, buffer)).fire();
				} else {
					buffer.unlockBuffer();
				}
				extDown.outBuffer = null;
			}
			break;
		}
	}

	/**
	 * Receives the message body of a response. A {@link Response} event that
	 * has a message body can be followed by one or more {@link Write} events
	 * from downstream that contain the data. An {@link EndOfResponse} event
	 * signals the end of the message body.
	 * 
	 * @param event
	 *            the event with the data
	 * @throws InterruptedException
	 */
	@Handler
	public void onWrite(Write<ManagedBuffer<?>> event)
	        throws InterruptedException {
		if (!(event.getConnection() instanceof ExtExtension)) {
			return;
		}
		final ExtExtension extDown = (ExtExtension)event.getConnection();
		final DataConnection netConn = extDown.getUpstreamConnection();
		final HttpResponseEncoder encoder = extDown.encoder;

		Buffer in = event.getBuffer().getBacking();
		while (true) {
			HttpResponseEncoder.Result result = null;
			if (in instanceof ByteBuffer) {
				result = encoder.encode((ByteBuffer) in,
						extDown.outBuffer.getBacking(), false);
			}
			if (!result.isOverflow()) {
				break;
			}
			(new Write<>(netConn, extDown.outBuffer)).fire();
			extDown.outBuffer = netConn.acquireByteBuffer();
		}
	}

	/**
	 * Signals the end of a response message's body.
	 * 
	 * @param event
	 *            the event
	 * @throws InterruptedException
	 */
	@Handler
	public void onEndOfResponse(EndOfResponse event)
	        throws InterruptedException {
		if (!(event.getConnection() instanceof ExtExtension)) {
			return;
		}
		final ExtExtension extDown = (ExtExtension)event.getConnection();
		final DataConnection netConn = extDown.getUpstreamConnection();
		flush(netConn);
	}

	/**
	 * Handles a close event from downstream by flushing any remaining data and
	 * sending a {@link Close} event upstream.
	 * 
	 * @param event
	 *            the close event
	 * @throws InterruptedException
	 */
	@Handler
	public void onClose(Close<?> event) throws InterruptedException {
		final DataConnection netConn = ((Extension) event.getConnection())
		        .getUpstreamConnection();
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
		final ExtExtension extDown = (ExtExtension) Extension
		        .lookupExtension(netConn);
		final HttpResponseEncoder encoder = extDown.encoder;

		// Send remaining data
		while (true) {
			final ManagedByteBuffer buffer = extDown.outBuffer;
			HttpResponseEncoder.Result result = encoder
			        .encode(buffer.getBacking());
			if (!result.isOverflow()) {
				if (buffer.position() > 0) {
					(new Write<>(netConn, buffer)).fire();
				} else {
					buffer.unlockBuffer();
				}
				extDown.outBuffer = null;
				break;
			}
			(new Write<>(netConn, buffer)).fire();
			extDown.outBuffer = netConn.acquireByteBuffer();
		}
	}

	/**
	 * Checks whether the request has been handled (status code of response has
	 * been set). If not, send the default response ("Not implemented") to the
	 * client.
	 * 
	 * @param event
	 *            the request completed event
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
	 * Provides a fallback handler for an OPTIONS request with asterisk. Simply
	 * responds with "OK".
	 * 
	 * @param event
	 * @throws ParseException
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onOptions(OptionsRequest event) throws ParseException {
		if (event.getRequestUri() == HttpRequest.ASTERISK_REQUEST) {
			HttpResponse response = event.getRequest().getResponse();
			response.setStatus(HttpStatus.OK);
			fire(new Response(event.getConnection(), response));
			event.stop();
		}
	}

	/**
	 * Provides a fallback handler (lowest priority) for the request types
	 * specified in the constructor.
	 * 
	 * @param event
	 * @throws ParseException
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onRequest(Request event) throws ParseException {
		if (providedFallbacks == null
		        || !providedFallbacks.contains(event.getClass())) {
			return;
		}
		final HttpResponse response = event.getRequest().getResponse();
		final DataConnection connection = event.getConnection();
		response.setStatus(HttpStatus.NOT_FOUND);
		response.setMessageHasBody(true);
		HttpMediaTypeField media = new HttpMediaTypeField(
		        HttpField.CONTENT_TYPE, "text", "plain");
		media.setParameter("charset", "utf-8");
		response.setField(media);
		(new Response(connection, response)).fire();
		try {
			Write.wrap(connection,
			        "Not Found".getBytes("utf-8")).fire();
		} catch (UnsupportedEncodingException e) {
		}
		(new EndOfResponse(connection)).fire();
		event.stop();
	}
}
