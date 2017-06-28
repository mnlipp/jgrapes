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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;

import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.ServerEngine;
import org.jdrupes.httpcodec.protocols.http.HttpConstants.HttpStatus;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder;
import org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder;
import org.jdrupes.httpcodec.protocols.websocket.WsCloseFrame;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.types.Converters;
import org.jdrupes.httpcodec.types.MediaType;
import org.jdrupes.httpcodec.types.StringList;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.events.OptionsRequest;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.Upgraded;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.http.events.WebSocketClosed;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferQueue;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.io.util.ManagedCharBuffer;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Accepted;

/**
 * A converter component that receives and sends byte buffers on a 
 * network channel and sends HTTP requests and receives HTTP 
 * responses on its own channel.
 */
public class HttpServer extends Component {

	private List<Class<? extends Request>> providedFallbacks;
	private int matchLevels = 1;
	boolean acceptNoSni = false;
	int applicationBufferSize = -1;

	/**
	 * Create a new server that uses the {@code networkChannel} for network
	 * level I/O.
	 * <P>
	 * As a convenience the server can provide fall back handlers for the
	 * specified types of requests. The fall back handler simply returns 404 (
	 * "Not found").
	 * 
	 * @param appChannel
	 *            this component's channel
	 * @param networkChannel
	 *            the channel for network level I/O
	 * @param fallbacks
	 *            the requests for which a fall back handler is provided
	 */
	@SafeVarargs
	public HttpServer(Channel appChannel, Channel networkChannel,
	        Class<? extends Request>... fallbacks) {
		super(appChannel);
		this.providedFallbacks = Arrays.asList(fallbacks);
		Handler.Evaluator.add(
				this, "onAccepted", networkChannel.defaultCriterion());
		Handler.Evaluator.add(
				this, "onInput", networkChannel.defaultCriterion());
		Handler.Evaluator.add(
				this, "onClosed", networkChannel.defaultCriterion());
	}

	/**
	 * Create a new server that creates its own {@link TcpServer} with the given
	 * address and uses it for network level I/O.
	 * 
	 * @param appChannel
	 *            this component's channel
	 * @param serverAddress the address to listen on
	 * @param fallbacks fall backs
	 */
	@SafeVarargs
	public HttpServer(Channel appChannel, SocketAddress serverAddress,
	        Class<? extends Request>... fallbacks) {
		super(appChannel);
		this.providedFallbacks = Arrays.asList(fallbacks);
		TcpServer server = new TcpServer().setServerAddress(serverAddress);
		attach(server);
		Handler.Evaluator.add(
				this, "onAccepted", server.defaultCriterion());
		Handler.Evaluator.add(
				this, "onInput", server.defaultCriterion());
	}

	/**
	 * @return the matchLevels
	 */
	public int matchLevels() {
		return matchLevels;
	}

	/**
	 * Sets the number of elements from the request path used in the match value
	 * of the generated events (see {@link Request#defaultCriterion()}), defaults
	 * to 1.
	 * 
	 * @param matchLevels the matchLevels to set
	 * @return the http server for easy chaining
	 */
	public HttpServer setMatchLevels(int matchLevels) {
		this.matchLevels = matchLevels;
		return this;
	}

	/**
	 * Sets the size of the buffers used for {@link Output} events
	 * on the application channel. Defaults to the upstream buffer size
	 * minus 512 (estimate for added protocol overhead).
	 * 
	 * @param applicationBufferSize the size to set
	 * @return the http server for easy chaining
	 */
	public HttpServer setApplicationBufferSize(int applicationBufferSize) {
		this.applicationBufferSize = applicationBufferSize;
		return this;
	}

	/**
	 * Returns the size of the application side (receive) buffers.
	 * 
	 * @return the value or -1 if not set
	 */
	public int applicationBufferSize() {
		return applicationBufferSize;
	}

	/**
	 * Determines if request from secure (TLS) connections without
	 * SNI are accepted.
	 *  
	 * Secure (TLS) requests usually transfer the name of the server that
	 * they want to connect to during handshake. The HTTP server checks
	 * that the `Host` header field of decoded requests matches the
	 * name used to establish the connection. If, however, the connection
	 * is made using the IP-address, the client does not have a host name.
	 * If such connections are to be accepted, this flag, which
	 * defaults to `false`, must be set.
	 * 
	 * Note that in request accepted without SNI, the `Host` header field
	 * will be modified to contain the IP-address of the indicated host
	 * to prevent accidental matching wit virtual host names.  
	 * 
	 * @param acceptNoSni the value to set
	 * @return the http server for easy chaining
	 */
	public HttpServer setAcceptNoSni(boolean acceptNoSni) {
		this.acceptNoSni = acceptNoSni;
		return this;
	}

	/**
	 * Returns if secure (TLS) requests without SNI are allowed.
	 * 
	 * @return the result
	 */
	public boolean acceptNoSni() {
		return acceptNoSni;
	}

	/**
	 * Creates a new downstream connection as {@link LinkedIOSubchannel} 
	 * of the network connection, a {@link HttpRequestDecoder} and a
	 * {@link HttpResponseEncoder}.
	 * 
	 * @param event
	 *            the accepted event
	 */
	@Handler(dynamic=true)
	public void onAccepted(Accepted event, IOSubchannel netChannel) {
		new AppChannel(event, netChannel);
	}

	/**
	 * Handles data from the client (from upstream). The data is send through 
	 * the {@link HttpRequestDecoder} and events are sent downstream according
	 * to the decoding results.
	 * 
	 * @param event the event
	 * @throws ProtocolException if a protocol exception occurs
	 * @throws InterruptedException 
	 */
	@Handler(dynamic=true)
	public void onInput(
			Input<ManagedByteBuffer> event, IOSubchannel netChannel)
					throws ProtocolException, InterruptedException {
		@SuppressWarnings("unchecked")
		final Optional<AppChannel> appChannel = (Optional<AppChannel>)
				LinkedIOSubchannel.downstreamChannel(this, netChannel);
		if (appChannel.isPresent()) {
			appChannel.get().handleNetInput(event);
		}
	}

	@Handler(dynamic=true)
	public void onClosed(Closed event, IOSubchannel netChannel) {
		@SuppressWarnings("unchecked")
		final Optional<AppChannel> appChannel = (Optional<AppChannel>)
				LinkedIOSubchannel.downstreamChannel(this, netChannel);
		if (appChannel.isPresent()) {
			appChannel.get().handleClosed(event);
		}
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
	public void onResponse(Response event, AppChannel appChannel)
			throws InterruptedException {
		appChannel.handleResponse(event);
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
	public void onOutput(Output<ManagedBuffer<?>> event, AppChannel appChannel)
	        throws InterruptedException {
		appChannel.handleAppOutput(event);
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
	public void onClose(Close event, AppChannel appChannel) 
			throws InterruptedException {
		final IOSubchannel netChannel = appChannel.upstreamChannel();
		netChannel.respond(new Close());
	}

	/**
	 * Checks whether the request has been handled (status code of response has
	 * been set). If not, send the default response ("Not implemented") to the
	 * client.
	 * 
	 * @param event
	 *            the request completed event
	 * @param appChannel the application channel 
	 * @throws InterruptedException if the execution was interrupted
	 */
	@Handler
	public void onRequestCompleted(
			Request.Completed event, IOSubchannel appChannel)
	        throws InterruptedException {
		final Request requestEvent = event.event();
		if (requestEvent.isStopped()) {
			// Has been handled
			return;
		}
		final HttpResponse response 
			= requestEvent.httpRequest().response().get();

		if (response.statusCode() != HttpStatus.NOT_IMPLEMENTED.statusCode()) {
			// Some other component takes care
			return;
		}

		// No component has taken care of the request, provide
		// fallback response
		sendResponse(response, appChannel, 
				HttpStatus.NOT_IMPLEMENTED.statusCode(), 
				HttpStatus.NOT_IMPLEMENTED.reasonPhrase());
	}

	/**
	 * Provides a fallback handler for an OPTIONS request with asterisk. Simply
	 * responds with "OK".
	 * 
	 * @param event the event
	 * @param appChannel the application channel
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onOptions(OptionsRequest event, IOSubchannel appChannel) {
		if (event.requestUri() == HttpRequest.ASTERISK_REQUEST) {
			HttpResponse response = event.httpRequest().response().get();
			response.setStatus(HttpStatus.OK);
			appChannel.respond(new Response(response));
			event.stop();
		}
	}

	/**
	 * Provides a fall back handler (lowest priority) for the request types
	 * specified in the constructor.
	 * 
	 * @param event the event
	 * @param appChannel the application channel
	 * @throws ParseException if the request contains illegal header fields
	 */
	@Handler(priority = Integer.MIN_VALUE)
	public void onRequest(Request event, IOSubchannel appChannel)
			throws ParseException {
		if (providedFallbacks == null
		        || !providedFallbacks.contains(event.getClass())) {
			return;
		}
		sendResponse(event.httpRequest().response().get(), appChannel, 
				HttpStatus.NOT_FOUND.statusCode(), 
				HttpStatus.NOT_FOUND.reasonPhrase());
		event.stop();
	}

	@Handler
	public void onWebSocketAccepted(
			WebSocketAccepted event, AppChannel appChannel) {
		appChannel.handleWebSocketAccepted(event, appChannel);
	}
	
	private void sendResponse(HttpResponse response, IOSubchannel appChannel,
			int statusCode, String reasonPhrase) {
		response.setStatusCode(statusCode).setReasonPhrase(reasonPhrase)
			.setHasPayload(true).setField(
					HttpField.CONTENT_TYPE,
					MediaType.builder().setType("text", "plain")
					.setParameter("charset", "utf-8").build());
		fire(new Response(response), appChannel);
		try {
			fire(Output.wrap((statusCode + " " + reasonPhrase)
					.getBytes("utf-8"), true), appChannel);
		} catch (UnsupportedEncodingException e) {
			// Supported by definition
		}
	}
	
	private class AppChannel extends LinkedIOSubchannel {
		// Starts as ServerEngine<HttpRequest,HttpResponse> but may change
		private ServerEngine<?,?> engine;
		private ManagedByteBuffer outBuffer;
		private boolean secure;
		private List<String> snis = Collections.emptyList();
		private ManagedBufferQueue<ManagedByteBuffer, ByteBuffer> byteBufferPool;
		private ManagedBufferQueue<ManagedCharBuffer, CharBuffer> charBufferPool;
		private ManagedBufferQueue<?, ?> currentPool = null;
		private boolean switchedToWebSocket = false;
		private WsMessageHeader currentWsMessage = null;

		public AppChannel(Accepted event, IOSubchannel netChannel) {
			super(HttpServer.this, netChannel);
			engine = new ServerEngine<>(
					new HttpRequestDecoder(), new HttpResponseEncoder());
			secure = event.isSecure();
			if (secure) {
				snis = new ArrayList<>();
				for (SNIServerName sni: event.requestedServerNames()) {
					if (sni instanceof SNIHostName) {
						snis.add(((SNIHostName)sni).getAsciiName());
					}
				}
			}

			// Calculate "good" application buffer size
			int bufferSize = applicationBufferSize;
			if (bufferSize <= 0) {
				bufferSize = netChannel.byteBufferPool().bufferSize() - 512;
				if (bufferSize < 4096) {
					bufferSize = 4096;
				}
			}
			
			// Allocate buffer pools
			final int bufSize = bufferSize;
			byteBufferPool = new ManagedBufferQueue<>(ManagedByteBuffer::new,	
					() -> { return ByteBuffer.allocate(bufSize); }, 2);
			charBufferPool = new ManagedBufferQueue<>(ManagedCharBuffer::new,	
					() -> { return CharBuffer.allocate(bufSize); }, 2);
		}
		
		public void handleNetInput(Input<ManagedByteBuffer> event) 
				throws ProtocolException, InterruptedException {
			// Send the data from the event through the decoder.
			ByteBuffer in = event.buffer().backingBuffer();
			ManagedBuffer<?> bodyData = null;
			while (in.hasRemaining()) {
				Decoder.Result<?> result = engine.decode(in,
				        bodyData == null ? null : bodyData.backingBuffer(),
				        event.isEndOfRecord());
				if (result.response().isPresent()) {
					// Feedback required, send it
					respond(new Response(result.response().get()));
					if (result.response().get().isFinal()) {
						if (result.isHeaderCompleted()) {
							engine.currentRequest()
							.filter(WsCloseFrame.class::isInstance)
							.ifPresent(closeFrame -> {
								fire(new WebSocketClosed(
										(WsCloseFrame)closeFrame, AppChannel.this));
							});
						}
						break;
					}
					if (result.isResponseOnly()) {
						continue;
					}
				}
				if (result.isHeaderCompleted()) {
					if (!handleRequestHeader(engine.currentRequest().get())) {
						break;
					}
				}
				if (bodyData != null && bodyData.position() > 0) {
					bodyData.flip();
					fire(new Input<>(bodyData, !result.isOverflow() 
							&& !result.isUnderflow()), this);
				}
				if (result.isOverflow()) {
					// Determine what kind of buffer we need
					bodyData = currentPool.acquire();
					continue;
				}
			}
		}

		private boolean handleRequestHeader(MessageHeader request) {
			if (request instanceof HttpRequest) {
				HttpRequest httpRequest = (HttpRequest)request;
				if (httpRequest.hasPayload()) {
					if(httpRequest.findValue(
						HttpField.CONTENT_TYPE, Converters.MEDIA_TYPE)
							.map(f -> f.value().topLevelType()
									.equalsIgnoreCase("text"))
							.orElse(false)) {
						currentPool = charBufferPool;
					} else {
						currentPool = byteBufferPool;
					}
				}
				if (secure) {
					if (!snis.contains(httpRequest.host())) {
						if (acceptNoSni && snis.isEmpty()) {
							convertHostToNumerical(httpRequest);
						} else {
							sendResponse(httpRequest.response().get(),
							        this, 421, "Misdirected Request");
							return false;
						}
					}
				}
				fire(Request.fromHttpRequest(httpRequest,
				        secure, matchLevels), this);
			} else if (request instanceof WsMessageHeader) {
				WsMessageHeader wsMessage = (WsMessageHeader)request;
				if (wsMessage.hasPayload()) {
					if (wsMessage.isTextMode()) {
						currentPool = charBufferPool;
					} else {
						currentPool = byteBufferPool;
					}
				}
			}
			return true;
		}
		
		private void convertHostToNumerical(HttpRequest request) {
			int port = request.port();
			String host;
			try {
				InetAddress addr = InetAddress.getByName(
						request.host());
				host = addr.getHostAddress();
				if (!(addr instanceof Inet4Address)) {
					host = "[" + host + "]";
				}
			} catch (UnknownHostException e) {
				host = "127.0.0.1";
			}
			request.setHostAndPort(host, port);
		}

		public void handleResponse(Response event) throws InterruptedException {
			if (!engine.encoding().isAssignableFrom(event.response().getClass())) {
				return;
			}
			final MessageHeader response = event.response();
			// Start sending the response
			@SuppressWarnings("unchecked")
			ServerEngine<?,MessageHeader> httpEngine 
				= (ServerEngine<?,MessageHeader>)engine;
			httpEngine.encode(response);
			boolean hasBody = response.hasPayload();
			while (true) {
				outBuffer = upstreamChannel().byteBufferPool().acquire();
				final ManagedByteBuffer buffer = outBuffer;
				Codec.Result result = engine.encode(
						Codec.EMPTY_IN, buffer.backingBuffer(), !hasBody);
				if (result.isOverflow()) {
					fire(new Output<>(buffer, false), upstreamChannel());
					continue;
				}
				if (hasBody) {
					// Keep buffer with incomplete response to be further
					// filled by Output events
					break;
				}
				// Response is complete
				if (buffer.position() > 0) {
					fire(new Output<>(buffer, false), upstreamChannel());
				} else {
					buffer.unlockBuffer();
				}
				outBuffer = null;
				if (result.closeConnection()) {
					fire(new Close(), upstreamChannel());
				}
				break;
			}
			
		}
		
		public void handleWebSocketAccepted(
				WebSocketAccepted event, AppChannel appChannel) {
			switchedToWebSocket = true;
			final HttpResponse response = event.requestEvent()
					.httpRequest().response().get()
					.setStatus(HttpStatus.SWITCHING_PROTOCOLS)
					.setField(HttpField.UPGRADE, new StringList("websocket"));
			event.addCompletedEvent(
					new Upgraded(event.resourceName(), "websocket"));
			respond(new Response(response));
		}

		public void handleAppOutput(Output<ManagedBuffer<?>> event) 
				throws InterruptedException {
			Buffer eventData = event.buffer().backingBuffer();
			Buffer input;
			if (eventData instanceof ByteBuffer) {
				input = ((ByteBuffer)eventData).duplicate();
			} else if(eventData instanceof CharBuffer) {
				input = ((CharBuffer)eventData).duplicate();
			} else {
				return;
			}
			if (switchedToWebSocket && currentWsMessage == null) {
				// When switched to WebSockets, we only have Input and Output
				// events. Add header automatically.
				@SuppressWarnings("unchecked")
				ServerEngine<?,MessageHeader> wsEngine 
					= (ServerEngine<?,MessageHeader>)engine;
				currentWsMessage = new WsMessageHeader(
						event.buffer().backingBuffer() instanceof CharBuffer,
						true);
				wsEngine.encode(currentWsMessage);
			}
			while (input.hasRemaining() || event.isEndOfRecord()) {
				if (outBuffer == null) {
					outBuffer = upstreamChannel().byteBufferPool().acquire();
				}
				Codec.Result result = engine.encode(input,
				        outBuffer.backingBuffer(), event.isEndOfRecord());
				if (result.isOverflow()) {
					upstreamChannel().respond(new Output<>(outBuffer, false));
					outBuffer = upstreamChannel().byteBufferPool().acquire();
					continue;
				}
				if (event.isEndOfRecord() || result.closeConnection()) {
					if (outBuffer.position() > 0) {
						upstreamChannel().respond(new Output<>(outBuffer, false));
					} else {
						outBuffer.unlockBuffer();
					}
					outBuffer = null;
					if (result.closeConnection()) {
						upstreamChannel().respond(new Close());
					}
					break;
				}
			}
			if (switchedToWebSocket && event.isEndOfRecord()) {
				currentWsMessage = null;
			}
		}
		
		public void handleClosed(Closed event) {
			fire(new Closed(), this);
		}

	}

}
