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

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.fields.HttpField;
import org.jdrupes.httpcodec.protocols.http.fields.HttpMediaTypeField;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.DynamicHandler;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.ConnectRequest;
import org.jgrapes.http.events.DeleteRequest;
import org.jgrapes.http.events.EndOfRequest;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.HeadRequest;
import org.jgrapes.http.events.OptionsRequest;
import org.jgrapes.http.events.PostRequest;
import org.jgrapes.http.events.PutRequest;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.TraceRequest;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.BufferCollector;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.Server;
import org.jgrapes.net.events.Accepted;

/**
 * @author Michael N. Lipp
 *
 */
public class HttpServer extends Component {

	private class DownSubchannel extends LinkedIOSubchannel {
		public ServerEngine<HttpRequest,HttpResponse> engine;
		public ManagedByteBuffer outBuffer;

		public DownSubchannel(IOSubchannel upstreamChannel) {
			super(HttpServer.this, upstreamChannel);
			engine = new ServerEngine<>
				(new HttpRequestDecoder(), new HttpResponseEncoder());
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
		addHandler("onInput", networkChannel.getMatchKey());
	}

	/**
	 * Create a new server that creates its own {@link Server} with the given
	 * address and uses it for network level I/O.
	 * 
	 * @param componentChannel
	 *            this component's channel
	 * @param serverAddress the address to listen on
	 * @param fallbacks fall backs
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
		addHandler("onInput", networkChannel.getMatchKey());
	}

	/**
	 * Creates a new downstream connection as {@link LinkedIOSubchannel} of the network
	 * connection, a {@link HttpRequestDecoder} and a
	 * {@link HttpResponseEncoder}.
	 * 
	 * @param event
	 *            the accepted event
	 */
	@DynamicHandler
	public void onAccepted(Accepted event) {
		for (IOSubchannel channel: event.channels(IOSubchannel.class)) {
			new DownSubchannel(channel);
		}
	}

	/**
	 * Handles data from the client (from upstream). The data is send through 
	 * the {@link HttpRequestDecoder} and events are sent downstream according
	 * to the decoding results.
	 * 
	 * @param event the event
	 * @throws ProtocolException if a protocol exception occurs
	 */
	@DynamicHandler
	public void onInput(Input<ManagedByteBuffer> event) 
			throws ProtocolException {
		IOSubchannel netChannel = event.firstChannel(IOSubchannel.class);
		final DownSubchannel downChannel = (DownSubchannel) LinkedIOSubchannel
		        .lookupLinked(netChannel);
		// Get data associated with the channel
		final ServerEngine<HttpRequest,HttpResponse> engine 
			= downChannel.engine;
		if (engine == null) {
			throw new IllegalStateException(
			        "Read event for unknown connection.");
		}

		// Send the data from the event through the decoder.
		ByteBuffer in = event.getBuffer().getBacking();
		ManagedByteBuffer bodyData = null;
		while (in.hasRemaining()) {
			Decoder.Result<HttpResponse> result = engine.decode(in,
			        bodyData == null ? null : bodyData.getBacking(),
			        event.isEndOfRecord());
			if (result.isHeaderCompleted()) {
				fireRequest(engine.currentRequest().get(), downChannel);
			}
			if (result.hasResponse()) {
				// Error during decoding, send back
				fire(new Response(result.getResponse()), downChannel);
				break;
			}
			if (bodyData != null && bodyData.position() > 0) {
				fire(new Input<>(bodyData, !result.isOverflow() 
						&& !result.isUnderflow()), downChannel);
			}
			if (result.isOverflow()) {
				bodyData = new ManagedByteBuffer(
				        ByteBuffer.allocate(in.capacity()),
				        BufferCollector.NOOP_COLLECTOR);
				continue;
			}
			if (!result.isUnderflow()
			        && engine.currentRequest().get().messageHasBody()) {
				fire(new EndOfRequest(), downChannel);
			}
		}
	}

	/**
	 * Creates a specific request event as appropriate for the request and fires
	 * it.
	 * 
	 * @param request
	 *            the decoded request
	 * @param channel
	 *            the downstream channel
	 */
	private void fireRequest(HttpRequest request, Channel channel) {
		Request req;
		switch (request.getMethod()) {
		case "OPTIONS":
			req = new OptionsRequest(request);
			break;
		case "GET":
			req = new GetRequest(request);
			break;
		case "HEAD":
			req = new HeadRequest(request);
			break;
		case "POST":
			req = new PostRequest(request);
			break;
		case "PUT":
			req = new PutRequest(request);
			break;
		case "DELETE":
			req = new DeleteRequest(request);
			break;
		case "TRACE":
			req = new TraceRequest(request);
			break;
		case "CONNECT":
			req = new ConnectRequest(request);
			break;
		default:
			req = new Request(request);
			break;
		}
		fire(req, channel);
	}

	/**
	 * Handles a response event from downstream by sending it through an
	 * {@link HttpResponseEncoder} that generates the data (encoded information)
	 * and sends it upstream with {@link Output} events. Depending on whether 
	 * the response has a body, subsequent {@link Output} events can
	 * follow.
	 * 
	 * @param event
	 *            the response event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onResponse(Response event) throws InterruptedException {
		DownSubchannel downChannel = event.firstChannel(DownSubchannel.class);
		final IOSubchannel netChannel = downChannel.getUpstreamChannel();
		final ServerEngine<HttpRequest,HttpResponse> engine 
			= downChannel.engine;
		final HttpResponse response = event.getResponse();

		// Start sending the response
		engine.encode(response);
		while (true) {
			downChannel.outBuffer = netChannel.bufferPool().acquire();
			final ManagedByteBuffer buffer = downChannel.outBuffer;
			Codec.Result result = engine.encode
					(Codec.EMPTY_IN, buffer.getBacking(), false);
			if (result.isOverflow()) {
				fire(new Output<>(buffer, false), netChannel);
				continue;
			}
			if (response.messageHasBody()) {
				// Keep buffer with incomplete response to be further
				// filled by Output events
				break;
			}
			// Response is complete
			if (buffer.position() > 0) {
				fire(new Output<>(buffer, false), netChannel);
			} else {
				buffer.unlockBuffer();
			}
			downChannel.outBuffer = null;
			break;
		}
	}

	/**
	 * Receives the message body of a response. A {@link Response} event that
	 * has a message body can be followed by one or more {@link Output} events
	 * from downstream that contain the data. An {@code Output} event
	 * with the end of record flag set signals the end of the message body.
	 * 
	 * @param event
	 *            the event with the data
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onOutput(Output<ManagedBuffer<?>> event)
	        throws InterruptedException {
		DownSubchannel downChannel = event.firstChannel(DownSubchannel.class);
		final IOSubchannel netChannel = downChannel.getUpstreamChannel();
		final ServerEngine<HttpRequest,HttpResponse> engine 
			= downChannel.engine;

		Buffer in = event.getBuffer().getBacking();
		if (!(in instanceof ByteBuffer)) {
			return;
		}
		while (true) {
			Codec.Result result = engine.encode((ByteBuffer) in,
			        downChannel.outBuffer.getBacking(), event.isEndOfRecord());
			if (!result.isOverflow() && !event.isEndOfRecord()) {
				break;
			}
			fire(new Output<>(downChannel.outBuffer, false), netChannel);
			if (event.isEndOfRecord()) {
				downChannel.outBuffer = null;
				break;
			}
			downChannel.outBuffer = netChannel.bufferPool().acquire();
		}
	}

	/**
	 * Handles a close event from downstream by sending a {@link Close} 
	 * event upstream.
	 * 
	 * @param event
	 *            the close event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onClose(Close event) throws InterruptedException {
		DownSubchannel downChannel = event.firstChannel(DownSubchannel.class);
		final IOSubchannel netChannel = downChannel.getUpstreamChannel();
		netChannel.fire(new Close());
	}

	/**
	 * Checks whether the request has been handled (status code of response has
	 * been set). If not, send the default response ("Not implemented") to the
	 * client.
	 * 
	 * @param event
	 *            the request completed event
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onRequestCompleted(Request.Completed event)
	        throws InterruptedException {
		IOSubchannel channel = event.getCompleted()
		        .firstChannel(IOSubchannel.class);
		final Request requestEvent = event.getCompleted();
		final HttpResponse response 
			= requestEvent.getRequest().getResponse().get();

		if (response.getStatusCode() == HttpStatus.NOT_IMPLEMENTED
		        .getStatusCode()) {
			channel.fire(new Response(response));
		}
	}

	/**
	 * Provides a fallback handler for an OPTIONS request with asterisk. Simply
	 * responds with "OK".
	 * 
	 * @param event the event
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onOptions(OptionsRequest event) {
		IOSubchannel channel = event.firstChannel(IOSubchannel.class);
		
		if (event.getRequestUri() == HttpRequest.ASTERISK_REQUEST) {
			HttpResponse response = event.getRequest().getResponse().get();
			response.setStatus(HttpStatus.OK);
			channel.fire(new Response(response));
			event.stop();
		}
	}

	/**
	 * Provides a fall back handler (lowest priority) for the request types
	 * specified in the constructor.
	 * 
	 * @param event the event
	 * @throws ParseException if the request contains illegal header fields
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onRequest(Request event) throws ParseException {
		if (providedFallbacks == null
		        || !providedFallbacks.contains(event.getClass())) {
			return;
		}
		IOSubchannel channel = event.firstChannel(IOSubchannel.class);
		
		final HttpResponse response = event.getRequest().getResponse().get();
		response.setStatus(HttpStatus.NOT_FOUND);
		response.setMessageHasBody(true);
		HttpMediaTypeField media = new HttpMediaTypeField(
		        HttpField.CONTENT_TYPE, "text", "plain");
		media.setParameter("charset", "utf-8");
		response.setField(media);
		fire(new Response(response), channel);
		try {
			fire(Output.wrap("Not Found".getBytes("utf-8"), true), channel);
		} catch (UnsupportedEncodingException e) {
		}
		event.stop();
	}
}
