/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.http;

import java.lang.ref.WeakReference;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import org.jdrupes.httpcodec.types.StringList;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.core.internal.EventProcessor;
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.Upgraded;
import org.jgrapes.http.events.WebSocketClose;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.Purge;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Accepted;

/**
 * A converter component that receives and sends byte buffers on a 
 * network channel and web application layer messages on
 * {@link IOSubchannel}s of its channel. 
 * 
 * Each {@link IOSubchannel} represents a connection established by 
 * the browser. The {@link HttpServer} fires {@link Request} events 
 * (and {@link Input} events, if there is associated data) on the 
 * subchannels. Web application components (short "weblets") handle 
 * these events and use 
 * {@link LinkedIOSubchannel#respond(org.jgrapes.core.Event)}
 * to send {@link Response} events and, if applicable, {@link Output}
 * events with data belonging to the response.
 * 
 * Events must be fired by weblets while handling the {@link Request}
 * or {@link Input} events only (to be precise: while handling events 
 * processed by the associated {@link EventProcessor}) to ensure
 * that responses and their associated data do not interleave. 
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class HttpServer extends Component {

    private WeakReference<Channel> networkChannelPassBack;
    private List<Class<? extends Request.In>> providedFallbacks;
    private int matchLevels = 1;
    private boolean acceptNoSni;
    private int applicationBufferSize = -1;

    /**
     * Denotes the network channel in handler annotations.
     */
    private static class NetworkChannel extends ClassChannel {
    }

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
            Class<? extends Request.In>... fallbacks) {
        super(appChannel, ChannelReplacements.create()
            .add(NetworkChannel.class, networkChannel));
        networkChannelPassBack = new WeakReference<Channel>(networkChannel);
        this.providedFallbacks = Arrays.asList(fallbacks);
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
    public HttpServer(Channel appChannel, InetSocketAddress serverAddress,
            Class<? extends Request.In>... fallbacks) {
        this(appChannel, new TcpServer().setServerAddress(serverAddress),
            fallbacks);
        attach((TcpServer) networkChannelPassBack.get());
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
     * to prevent accidental matching with virtual host names.  
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
    @Handler(channels = NetworkChannel.class)
    public void onAccepted(Accepted event, IOSubchannel netChannel) {
        new WebAppMsgChannel(event, netChannel);
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
    @Handler(channels = NetworkChannel.class)
    public void onInput(
            Input<ByteBuffer> event, IOSubchannel netChannel)
            throws ProtocolException, InterruptedException {
        @SuppressWarnings("unchecked")
        final Optional<WebAppMsgChannel> appChannel
            = (Optional<WebAppMsgChannel>) LinkedIOSubchannel
                .downstreamChannel(this, netChannel);
        if (appChannel.isPresent()) {
            appChannel.get().handleNetInput(event);
        }
    }

    /**
     * Forwards a {@link Closed} event to the application channel. 
     *
     * @param event the event
     * @param netChannel the net channel
     */
    @Handler(channels = NetworkChannel.class)
    public void onClosed(Closed event, IOSubchannel netChannel) {
        LinkedIOSubchannel.downstreamChannel(this, netChannel,
            WebAppMsgChannel.class).ifPresent(appChannel -> {
                appChannel.handleClosed(event);
            });
    }

    /**
     * Forwards a {@link Purge} event to the application channel.
     *
     * @param event the event
     * @param netChannel the net channel
     */
    @Handler(channels = NetworkChannel.class)
    public void onPurge(Purge event, IOSubchannel netChannel) {
        LinkedIOSubchannel.downstreamChannel(this, netChannel,
            WebAppMsgChannel.class).ifPresent(appChannel -> {
                appChannel.handlePurge(event);
            });
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
    public void onResponse(Response event, WebAppMsgChannel appChannel)
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
    public void onOutput(Output<?> event, WebAppMsgChannel appChannel)
            throws InterruptedException {
        appChannel.handleAppOutput(event);
    }

    /**
     * Handles a close event from downstream by closing the upstream
     * connections.
     * 
     * @param event
     *            the close event
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler
    public void onClose(Close event, WebAppMsgChannel appChannel)
            throws InterruptedException {
        appChannel.handleClose(event);
    }

    /**
     * Checks whether the request has been handled (value of {@link Request}
     * event set to `true`) or the status code in the prepared response
     * is no longer "Not Implemented". If not, but a fall back has been set, 
     * send a "Not Found" response. If this isn't the case either, send 
     * the default response ("Not implemented") to the client.
     * 
     * @param event
     *            the request completed event
     * @param appChannel the application channel 
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler
    public void onRequestCompleted(
            Request.In.Completed event, IOSubchannel appChannel)
            throws InterruptedException {
        final Request.In requestEvent = event.event();
        // A check that also works with null.
        if (Boolean.TRUE.equals(requestEvent.get())
            || requestEvent.httpRequest().response().map(
                response -> response.statusCode() != HttpStatus.NOT_IMPLEMENTED
                    .statusCode())
                .orElse(false)) {
            // Some other component has taken care
            return;
        }

        // Check if "Not Found" should be sent
        if (providedFallbacks != null
            && providedFallbacks.contains(requestEvent.getClass())) {
            ResponseCreationSupport.sendResponse(
                requestEvent.httpRequest(), appChannel, HttpStatus.NOT_FOUND);
            return;
        }

        // Last resort
        ResponseCreationSupport.sendResponse(requestEvent.httpRequest(),
            appChannel, HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Provides a fallback handler for an OPTIONS request with asterisk. Simply
     * responds with "OK".
     * 
     * @param event the event
     * @param appChannel the application channel
     */
    @Handler(priority = Integer.MIN_VALUE)
    public void onOptions(Request.In.Options event, IOSubchannel appChannel) {
        if (event.requestUri() == HttpRequest.ASTERISK_REQUEST) {
            HttpResponse response = event.httpRequest().response().get();
            response.setStatus(HttpStatus.OK);
            appChannel.respond(new Response(response));
            event.setResult(true);
            event.stop();
        }
    }

    /**
     * Send the response indicating that the protocol switch was accepted
     * and causes subsequent data to be handled as {@link Input} and
     * {@link Output} events on the channel.
     * 
     * As a convenience, the channel is associates with the URI that
     * was used to request the protocol switch using {@link URI} as key.
     * 
     * @param event the event
     * @param appChannel the channel
     */
    @Handler
    public void onProtocolSwitchAccepted(
            ProtocolSwitchAccepted event, WebAppMsgChannel appChannel) {
        appChannel.handleProtocolSwitchAccepted(event, appChannel);
    }

    /**
     * An application layer channel.
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private class WebAppMsgChannel extends LinkedIOSubchannel {
        // Starts as ServerEngine<HttpRequest,HttpResponse> but may change
        private final ServerEngine<?, ?> engine;
        private ManagedBuffer<ByteBuffer> outBuffer;
        private final boolean secure;
        private List<String> snis = Collections.emptyList();
        private final ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> byteBufferPool;
        private final ManagedBufferPool<ManagedBuffer<CharBuffer>,
                CharBuffer> charBufferPool;
        private ManagedBufferPool<?, ?> currentPool;
        private final EventPipeline downPipeline;
        private Upgraded pendingUpgraded;
        private WsMessageHeader currentWsMessage;

        /**
         * Instantiates a new channel.
         *
         * @param event the event
         * @param netChannel the net channel
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public WebAppMsgChannel(Accepted event, IOSubchannel netChannel) {
            super(HttpServer.this, channel(), netChannel, newEventPipeline());
            engine = new ServerEngine<>(
                new HttpRequestDecoder(), new HttpResponseEncoder());
            secure = event.isSecure();
            if (secure) {
                snis = new ArrayList<>();
                for (SNIServerName sni : event.requestedServerNames()) {
                    if (sni instanceof SNIHostName) {
                        snis.add(((SNIHostName) sni).getAsciiName());
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

            String channelName = Components.objectName(HttpServer.this)
                + "." + Components.objectName(this);
            byteBufferPool().setName(channelName + ".upstream.byteBuffers");
            charBufferPool().setName(channelName + ".upstream.charBuffers");
            // Allocate downstream buffer pools. Note that decoding WebSocket
            // network packets may result in several WS frames that are each
            // delivered in independent events. Therefore provide some
            // additional buffers.
            final int bufSize = bufferSize;
            byteBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocate(bufSize);
                }, 2, 100)
                    .setName(channelName + ".downstream.byteBuffers");
            charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return CharBuffer.allocate(bufSize);
                }, 2, 100)
                    .setName(channelName + ".downstream.charBuffers");

            // Downstream pipeline
            downPipeline = newEventPipeline();
        }

        /**
         * Handle {@link Input} events from the network.
         *
         * @param event the event
         * @throws ProtocolException the protocol exception
         * @throws InterruptedException the interrupted exception
         */
        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.AvoidDeeplyNestedIfStmts", "PMD.CollapsibleIfStatements" })
        public void handleNetInput(Input<ByteBuffer> event)
                throws ProtocolException, InterruptedException {
            // Send the data from the event through the decoder.
            ByteBuffer inData = event.data();
            // Don't unnecessary allocate a buffer, may be header only message
            ManagedBuffer<?> bodyData = null;
            boolean wasOverflow = false;
            while (inData.hasRemaining()) {
                if (wasOverflow) {
                    // Message has (more) body
                    bodyData = currentPool.acquire();
                }
                Decoder.Result<?> result = engine.decode(inData,
                    bodyData == null ? null : bodyData.backingBuffer(),
                    event.isEndOfRecord());
                if (result.response().isPresent()) {
                    // Feedback required, send it
                    responsePipeline().overrideRestriction().fire(
                        new Response(result.response().get()), this);
                    if (result.isResponseOnly()) {
                        maybeCloseConnection(result);
                        continue;
                    }
                }
                if (result.isHeaderCompleted()) {
                    if (!handleRequestHeader(engine.currentRequest().get())) {
                        maybeCloseConnection(result);
                        break;
                    }
                }
                if (bodyData != null) {
                    if (bodyData.position() > 0) {
                        downPipeline.fire(Input.fromSink(
                            bodyData, !result.isOverflow()
                                && !result.isUnderflow()),
                            this);
                    } else {
                        bodyData.unlockBuffer();
                    }
                    bodyData = null;
                }
                maybeCloseConnection(result);
                wasOverflow = result.isOverflow();
            }
        }

        private void maybeCloseConnection(Decoder.Result<?> result) {
            if (result.closeConnection()) {
                respond(new Close());
            }
        }

        @SuppressWarnings("PMD.CollapsibleIfStatements")
        private boolean handleRequestHeader(MessageHeader request) {
            if (request instanceof HttpRequest) {
                HttpRequest httpRequest = (HttpRequest) request;
                if (httpRequest.hasPayload()) {
                    if (httpRequest.findValue(
                        HttpField.CONTENT_TYPE, Converters.MEDIA_TYPE)
                        .map(type -> type.value().topLevelType()
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
                            ResponseCreationSupport.sendResponse(httpRequest,
                                this, 421, "Misdirected Request");
                            return false;
                        }
                    }
                }
                downPipeline.fire(Request.In.fromHttpRequest(httpRequest,
                    secure, matchLevels), this);
            } else if (request instanceof WsMessageHeader) {
                WsMessageHeader wsMessage = (WsMessageHeader) request;
                if (wsMessage.hasPayload()) {
                    if (wsMessage.isTextMode()) {
                        currentPool = charBufferPool;
                    } else {
                        currentPool = byteBufferPool;
                    }
                }
            } else if (request instanceof WsCloseFrame) {
                downPipeline.fire(
                    new WebSocketClose((WsCloseFrame) request, this));
            }
            return true;
        }

        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.UseStringBufferForStringAppends" })
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
                host = InetAddress.getLoopbackAddress().getHostAddress();
            }
            request.setHostAndPort(host, port);
        }

        /**
         * Handle a response event from the application layer.
         *
         * @param event the event
         * @throws InterruptedException the interrupted exception
         */
        @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.AvoidBranchingStatementAsLastInLoop" })
        public void handleResponse(Response event) throws InterruptedException {
            if (!engine.encoding()
                .isAssignableFrom(event.response().getClass())) {
                return;
            }
            final MessageHeader response = event.response();
            // Start sending the response
            @SuppressWarnings("unchecked")
            ServerEngine<?, MessageHeader> msgEngine
                = (ServerEngine<?, MessageHeader>) engine;
            msgEngine.encode(response);
            if (pendingUpgraded != null) {
                if (response instanceof HttpResponse
                    && ((HttpResponse) response).statusCode() % 100 == 1) {
                    downPipeline.fire(pendingUpgraded, this);
                }
                pendingUpgraded = null;
            }
            boolean hasBody = response.hasPayload();
            while (true) {
                outBuffer = upstreamChannel().byteBufferPool().acquire();
                final ManagedBuffer<ByteBuffer> buffer = outBuffer;
                Codec.Result result = engine.encode(
                    Codec.EMPTY_IN, buffer.backingBuffer(), !hasBody);
                if (result.isOverflow()) {
                    upstreamChannel().respond(Output.fromSink(buffer, false));
                    continue;
                }
                if (hasBody) {
                    // Keep buffer with incomplete response to be further
                    // filled by subsequent Output events
                    break;
                }
                // Response is complete
                if (buffer.position() > 0) {
                    upstreamChannel().respond(Output.fromSink(buffer, true));
                } else {
                    buffer.unlockBuffer();
                }
                outBuffer = null;
                if (result.closeConnection()) {
                    upstreamChannel().respond(new Close());
                }
                break;
            }

        }

        /**
         * Handle a {@link ProtocolSwitchAccepted} event from the 
         * application layer.
         *
         * @param event the event
         * @param appChannel the app channel
         */
        public void handleProtocolSwitchAccepted(
                ProtocolSwitchAccepted event, WebAppMsgChannel appChannel) {
            appChannel.setAssociated(URI.class,
                event.requestEvent().requestUri());
            final HttpResponse response = event.requestEvent()
                .httpRequest().response().get()
                .setStatus(HttpStatus.SWITCHING_PROTOCOLS)
                .setField(HttpField.UPGRADE,
                    new StringList(event.protocol()));
            // We send the Upgraded event only after the response has
            // successfully been encoded (and thus checked).
            pendingUpgraded = new Upgraded(event.resourceName(),
                event.protocol());
            respond(new Response(response));
        }

        /**
         * Handle output from the application layer.
         *
         * @param event the event
         * @throws InterruptedException the interrupted exception
         */
        @SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.NcssCount",
            "PMD.NPathComplexity", "PMD.AvoidInstantiatingObjectsInLoops" })
        public void handleAppOutput(Output<?> event)
                throws InterruptedException {
            Buffer eventData = event.data();
            Buffer input;
            if (eventData instanceof ByteBuffer) {
                input = ((ByteBuffer) eventData).duplicate();
            } else if (eventData instanceof CharBuffer) {
                input = ((CharBuffer) eventData).duplicate();
            } else {
                return;
            }
            if (engine.switchedTo().equals(Optional.of("websocket"))
                && currentWsMessage == null) {
                // When switched to WebSockets, we only have Input and Output
                // events. Add header automatically.
                @SuppressWarnings("unchecked")
                ServerEngine<?, MessageHeader> wsEngine
                    = (ServerEngine<?, MessageHeader>) engine;
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
                    upstreamChannel()
                        .respond(Output.fromSink(outBuffer, false));
                    outBuffer = upstreamChannel().byteBufferPool().acquire();
                    continue;
                }
                if (event.isEndOfRecord() || result.closeConnection()) {
                    if (outBuffer.position() > 0) {
                        upstreamChannel()
                            .respond(Output.fromSink(outBuffer, true));
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
            if (engine.switchedTo().equals(Optional.of("websocket"))
                && event.isEndOfRecord()) {
                currentWsMessage = null;
            }
        }

        /**
         * Handle a {@link Close} event from the application layer.
         *
         * @param event the event
         * @throws InterruptedException the interrupted exception
         */
        public void handleClose(Close event) throws InterruptedException {
            if (engine.switchedTo().equals(Optional.of("websocket"))) {
                fire(new Response(new WsCloseFrame(null, null)), this);
                return;
            }
            upstreamChannel().respond(new Close());
        }

        /**
         * Handle a {@link Closed} event from the network by forwarding
         * it to the application layer.
         *
         * @param event the event
         */
        public void handleClosed(Closed event) {
            downPipeline.fire(new Closed(), this);
        }

        /**
         * Handle a {@link Purge} event by forwarding it to the
         * application layer.
         *
         * @param event the event
         */
        public void handlePurge(Purge event) {
            downPipeline.fire(new Purge(), this);
        }

    }

}
