/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2024 Michael N. Lipp
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jdrupes.httpcodec.ClientEngine;
import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.client.HttpRequestEncoder;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
import org.jdrupes.httpcodec.protocols.websocket.WsCloseFrame;
import org.jdrupes.httpcodec.protocols.websocket.WsMessageHeader;
import org.jdrupes.httpcodec.types.Converters;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.Components.PoolingIndex;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.http.events.HostUnresolved;
import org.jgrapes.http.events.HttpConnected;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Response;
import org.jgrapes.http.events.WebSocketClose;
import org.jgrapes.io.IOSubchannel.DefaultIOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.OpenSocketConnection;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.net.SocketIOChannel;
import org.jgrapes.net.events.ClientConnected;

/**
 * A converter component that receives and sends web application
 * layer messages and byte buffers on associated network channels.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects" })
public class HttpConnector extends Component {

    private int applicationBufferSize = -1;
    private final Channel netMainChannel;
    private final Channel netSecureChannel;
    private final PoolingIndex<SocketAddress, SocketIOChannel> pooled
        = new PoolingIndex<>();

    /**
     * Denotes the network channel in handler annotations.
     */
    private static final class NetworkChannel extends ClassChannel {
    }

    /**
     * Create a new connector that uses the {@code networkChannel} for network
     * level I/O.
     * 
     * @param appChannel
     *            this component's channel
     * @param networkChannel
     *            the channel for network level I/O
     * @param secureChannel
     *            the channel for secure network level I/O
     */
    public HttpConnector(Channel appChannel, Channel networkChannel,
            Channel secureChannel) {
        super(appChannel, ChannelReplacements.create()
            .add(NetworkChannel.class, networkChannel, secureChannel));
        this.netMainChannel = networkChannel;
        this.netSecureChannel = secureChannel;
    }

    /**
     * Create a new connector that uses the {@code networkChannel} for network
     * level I/O.
     * 
     * @param appChannel
     *            this component's channel
     * @param networkChannel
     *            the channel for network level I/O
     */
    public HttpConnector(Channel appChannel, Channel networkChannel) {
        super(appChannel, ChannelReplacements.create()
            .add(NetworkChannel.class, networkChannel));
        this.netMainChannel = networkChannel;
        this.netSecureChannel = null;
    }

    /**
     * Sets the size of the buffers used for {@link Input} events
     * on the application channel. Defaults to the upstream buffer size
     * minus 512 (estimate for added protocol overhead).
     * 
     * @param applicationBufferSize the size to set
     * @return the http server for easy chaining
     */
    public HttpConnector setApplicationBufferSize(int applicationBufferSize) {
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
     * Starts the processing of a request from the application layer.
     * When a network connection has been established, the application
     * layer will be informed by a {@link HttpConnected} event, fired
     * on a subchannel that is created for the processing of this
     * request.
     *
     * @param event the request
     * @throws InterruptedException if processing is interrupted
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onRequest(Request.Out event)
            throws InterruptedException, IOException {
        new WebAppMsgChannel(event);
    }

    /**
     * Handles output from the application. This may be the payload
     * of e.g. a POST or data to be transferes on a websocket connection.
     *
     * @param event the event
     * @param appChannel the application layer channel
     * @throws InterruptedException the interrupted exception
     */
    @Handler
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void onOutput(Output<?> event, WebAppMsgChannel appChannel)
            throws InterruptedException {
        if (appChannel.httpConnector() == this) {
            appChannel.handleAppOutput(event);
        }
    }

    /**
     * Called when the network connection is established. Triggers the
     * further processing of the initial request.
     *
     * @param event the event
     * @param netConnChannel the network layer channel
     * @throws InterruptedException if the execution is interrupted
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = NetworkChannel.class)
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void onConnected(ClientConnected event,
            SocketIOChannel netConnChannel)
            throws InterruptedException, IOException {
        // Check if this is a response to our request
        var appChannel = event.openEvent().associated(WebAppMsgChannel.class)
            .filter(c -> c.httpConnector() == this);
        if (appChannel.isPresent()) {
            appChannel.get().connected(netConnChannel);
        }
    }

    /**
     * Handles I/O error events from the network layer.
     *
     * @param event the event
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = NetworkChannel.class)
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void onIoError(IOError event) throws IOException {
        for (Channel channel : event.channels()) {
            if (channel instanceof SocketIOChannel netConnChannel) {
                // Error while using established network connection
                Optional<WebAppMsgChannel> appChannel
                    = netConnChannel.associated(WebAppMsgChannel.class)
                        .filter(c -> c.httpConnector() == this);
                if (appChannel.isPresent()) {
                    // Error while using a network connection
                    appChannel.get().handleIoError(event, netConnChannel);
                    continue;
                }
                // Just in case...
                pooled.remove(netConnChannel.remoteAddress(), netConnChannel);
                continue;
            }

            // Error while trying to establish the network connection
            if (event.event() instanceof OpenSocketConnection connEvent) {
                connEvent.associated(WebAppMsgChannel.class)
                    .filter(c -> c.httpConnector() == this).ifPresent(c -> {
                        c.openError(event);
                    });
            }
        }
    }

    /**
     * Processes any input from the network layer.
     *
     * @param event the event
     * @param netConnChannel the network layer channel
     * @throws InterruptedException if the thread is interrupted
     * @throws ProtocolException if the protocol is violated
     */
    @Handler(channels = NetworkChannel.class)
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void onInput(Input<ByteBuffer> event, SocketIOChannel netConnChannel)
            throws InterruptedException, ProtocolException {
        Optional<WebAppMsgChannel> appChannel
            = netConnChannel.associated(WebAppMsgChannel.class)
                .filter(c -> c.httpConnector() == this);
        if (appChannel.isPresent()) {
            appChannel.get().handleNetInput(event, netConnChannel);
        }
    }

    /**
     * Called when the network connection is closed. 
     *
     * @param event the event
     * @param netConnChannel the net conn channel
     */
    @Handler(channels = NetworkChannel.class)
    public void onClosed(Closed<?> event, SocketIOChannel netConnChannel) {
        netConnChannel.associated(WebAppMsgChannel.class)
            .filter(c -> c.httpConnector() == this).ifPresent(
                appChannel -> appChannel.handleClosed(event));
        pooled.remove(netConnChannel.remoteAddress(), netConnChannel);
    }

    /**
     * Handles a close event from the application channel. Such an
     * event may only be fired if the connection has been upgraded
     * to a websocket connection.
     *
     * @param event the event
     * @param appChannel the application channel
     */
    @Handler
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void onClose(Close event, WebAppMsgChannel appChannel) {
        if (appChannel.httpConnector() == this) {
            appChannel.handleClose(event);
        }
    }

    /**
     * An application layer channel.
     */
    private class WebAppMsgChannel extends DefaultIOSubchannel {
        // Starts as ClientEngine<HttpRequest,HttpResponse> but may change
        private final ClientEngine<?, ?> engine
            = new ClientEngine<>(new HttpRequestEncoder(),
                new HttpResponseDecoder());
        private final InetSocketAddress serverAddress;
        private final Request.Out request;
        private ManagedBuffer<ByteBuffer> outBuffer;
        private ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> byteBufferPool;
        private ManagedBufferPool<ManagedBuffer<CharBuffer>,
                CharBuffer> charBufferPool;
        private ManagedBufferPool<?, ?> currentPool;
        private SocketIOChannel netConnChannel;
        private final EventPipeline downPipeline;
        private WsMessageHeader currentWsMessage;

        /**
         * Instantiates a new channel.
         *
         * @param event the event
         * @param netChannel the net channel
         * @throws InterruptedException 
         * @throws IOException 
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public WebAppMsgChannel(Request.Out event)
                throws InterruptedException, IOException {
            super(channel(), newEventPipeline());

            // Downstream pipeline, needed even if connection fails
            downPipeline = newEventPipeline();

            // Extract request data and check host
            request = event;
            var uri = request.requestUri();
            var port = uri.getPort();
            if (port == -1) {
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    port = 443;
                } else if ("http".equalsIgnoreCase(uri.getScheme())) {
                    port = 80;
                }
            }
            serverAddress = new InetSocketAddress(uri.getHost(), port);
            if (serverAddress.isUnresolved()) {
                downPipeline.fire(new HostUnresolved(event,
                    "Host cannot be resolved."), this);
                return;
            }

            // Re-use network connection, if possible
            SocketIOChannel recycled = pooled.poll(serverAddress);
            if (recycled != null) {
                connected(recycled);
                return;
            }

            // Fire on network channel (targeting the network connector)
            // as a follow up event (using the current pipeline).
            var useSecure = uri.getScheme().equalsIgnoreCase("https")
                && netSecureChannel != null;
            fire(new OpenSocketConnection(serverAddress)
                .setAssociated(WebAppMsgChannel.class, this),
                useSecure ? netSecureChannel : netMainChannel);
        }

        private HttpConnector httpConnector() {
            return HttpConnector.this;
        }

        /**
         * Error in response to trying to open a new TCP connection.
         *
         * @param event the event
         */
        public void openError(IOError event) {
            // Already removed from connecting by caller, simply forward.
            downPipeline.fire(IOError.duplicate(event), this);
        }

        /**
         * Error from established TCP connection.
         *
         * @param event the event
         * @param netConnChannel the network channel
         */
        public void handleIoError(IOError event,
                SocketIOChannel netConnChannel) {
            downPipeline.fire(IOError.duplicate(event), this);
        }

        /**
         * Sets the network connection channel for this application channel.
         *
         * @param netConnChannel the net conn channel
         * @throws InterruptedException the interrupted exception
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public final void connected(SocketIOChannel netConnChannel)
                throws InterruptedException, IOException {
            // Associate the network channel with this application channel
            this.netConnChannel = netConnChannel;
            netConnChannel.setAssociated(WebAppMsgChannel.class, this);
            request.connectedCallback().ifPresent(
                consumer -> consumer.accept(request, netConnChannel));

            // Estimate "good" application buffer size
            int bufferSize = applicationBufferSize;
            if (bufferSize <= 0) {
                bufferSize = netConnChannel.byteBufferPool().bufferSize() - 512;
                if (bufferSize < 4096) {
                    bufferSize = 4096;
                }
            }
            String channelName = Components.objectName(HttpConnector.this)
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

            sendMessageUpstream(request.httpRequest(), netConnChannel);

            // Forward Connected event downstream to e.g. start preparation
            // of output events for payload data.
            downPipeline.fire(new HttpConnected(request,
                netConnChannel.localAddress(), netConnChannel.remoteAddress()),
                this);
        }

        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.CognitiveComplexity", "PMD.AvoidDuplicateLiterals" })
        private void sendMessageUpstream(MessageHeader message,
                SocketIOChannel netConnChannel) {
            // Now send request as if it came from downstream (to
            // avoid confusion with output events that may be
            // generated in parallel, see below).
            responsePipeline().submit("SynchronizedResponse",
                new Callable<Void>() {

                    @Override
                    @SuppressWarnings({ "PMD.CommentRequired",
                        "PMD.AvoidBranchingStatementAsLastInLoop",
                        "PMD.AvoidDuplicateLiterals",
                        "PMD.AvoidInstantiatingObjectsInLoops" })
                    public Void call() throws InterruptedException {
                        @SuppressWarnings("unchecked")
                        ClientEngine<MessageHeader, MessageHeader> untypedEngine
                            = (ClientEngine<MessageHeader,
                                    MessageHeader>) engine;
                        untypedEngine.encode(message);
                        boolean hasBody = message.hasPayload();
                        while (true) {
                            outBuffer
                                = netConnChannel.byteBufferPool().acquire();
                            Codec.Result result
                                = engine.encode(Codec.EMPTY_IN,
                                    outBuffer.backingBuffer(), !hasBody);
                            if (result.isOverflow()) {
                                netConnChannel
                                    .respond(Output.fromSink(outBuffer, false));
                                continue;
                            }
                            if (hasBody) {
                                // Keep buffer with incomplete request to be
                                // further
                                // filled by subsequent Output events
                                break;
                            }
                            // Request is completely encoded
                            if (outBuffer.position() > 0) {
                                netConnChannel
                                    .respond(Output.fromSink(outBuffer, true));
                            } else {
                                outBuffer.unlockBuffer();
                            }
                            outBuffer = null;
                            if (result.closeConnection()) {
                                netConnChannel.respond(new Close());
                            }
                            break;
                        }
                        return null;
                    }
                });
        }

        @SuppressWarnings({ "PMD.CommentRequired", "PMD.CyclomaticComplexity",
            "PMD.NPathComplexity", "PMD.AvoidInstantiatingObjectsInLoops",
            "PMD.AvoidDuplicateLiterals", "PMD.CognitiveComplexity" })
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
                ClientEngine<MessageHeader, ?> wsEngine
                    = (ClientEngine<MessageHeader, ?>) engine;
                currentWsMessage = new WsMessageHeader(
                    event.buffer().backingBuffer() instanceof CharBuffer,
                    true);
                wsEngine.encode(currentWsMessage);
            }
            while (input.hasRemaining() || event.isEndOfRecord()) {
                if (outBuffer == null) {
                    outBuffer = netConnChannel.byteBufferPool().acquire();
                }
                Codec.Result result = engine.encode(input,
                    outBuffer.backingBuffer(), event.isEndOfRecord());
                if (result.isOverflow()) {
                    netConnChannel.respond(Output.fromSink(outBuffer, false));
                    outBuffer = netConnChannel.byteBufferPool().acquire();
                    continue;
                }
                if (event.isEndOfRecord() || result.closeConnection()) {
                    if (outBuffer.position() > 0) {
                        netConnChannel
                            .respond(Output.fromSink(outBuffer, true));
                    } else {
                        outBuffer.unlockBuffer();
                    }
                    outBuffer = null;
                    if (result.closeConnection()) {
                        netConnChannel.respond(new Close());
                    }
                    break;
                }
            }
            if (engine.switchedTo().equals(Optional.of("websocket"))
                && event.isEndOfRecord()) {
                currentWsMessage = null;
            }
        }

        @SuppressWarnings({ "PMD.CommentRequired",
            "PMD.DataflowAnomalyAnalysis", "PMD.CognitiveComplexity" })
        public void handleNetInput(Input<ByteBuffer> event,
                SocketIOChannel netConnChannel)
                throws InterruptedException, ProtocolException {
            // Send the data from the event through the decoder.
            ByteBuffer inData = event.data();
            // Don't unnecessary allocate a buffer, may be header only message
            ManagedBuffer<?> bodyData = null;
            boolean wasOverflow = false;
            Decoder.Result<?> result;
            while (inData.hasRemaining()) {
                if (wasOverflow) {
                    // Message has (more) body
                    bodyData = currentPool.acquire();
                }
                result = engine.decode(inData,
                    bodyData == null ? null : bodyData.backingBuffer(),
                    event.isEndOfRecord());
                if (result.response().isPresent()) {
                    sendMessageUpstream(result.response().get(),
                        netConnChannel);
                    if (result.isResponseOnly()) {
                        maybeReleaseConnection(result);
                        continue;
                    }
                }
                if (result.isHeaderCompleted()) {
                    MessageHeader header
                        = engine.responseDecoder().header().get();
                    if (!handleResponseHeader(header)) {
                        maybeReleaseConnection(result);
                        break;
                    }
                }
                if (bodyData != null) {
                    if (bodyData.position() > 0) {
                        boolean eor
                            = !result.isOverflow() && !result.isUnderflow();
                        downPipeline.fire(Input.fromSink(bodyData, eor), this);
                    } else {
                        bodyData.unlockBuffer();
                    }
                    bodyData = null;
                }
                maybeReleaseConnection(result);
                wasOverflow = result.isOverflow();
            }
        }

        @SuppressWarnings("PMD.CognitiveComplexity")
        private boolean handleResponseHeader(MessageHeader response) {
            if (response instanceof HttpResponse) {
                HttpResponse httpResponse = (HttpResponse) response;
                if (httpResponse.hasPayload()) {
                    if (httpResponse.findValue(
                        HttpField.CONTENT_TYPE, Converters.MEDIA_TYPE)
                        .map(type -> "text"
                            .equalsIgnoreCase(type.value().topLevelType()))
                        .orElse(false)) {
                        currentPool = charBufferPool;
                    } else {
                        currentPool = byteBufferPool;
                    }
                }
                downPipeline.fire(new Response(httpResponse), this);
            } else if (response instanceof WsMessageHeader) {
                WsMessageHeader wsMessage = (WsMessageHeader) response;
                if (wsMessage.hasPayload()) {
                    if (wsMessage.isTextMode()) {
                        currentPool = charBufferPool;
                    } else {
                        currentPool = byteBufferPool;
                    }
                }
            } else if (response instanceof WsCloseFrame) {
                downPipeline.fire(
                    new WebSocketClose((WsCloseFrame) response, this));
            }
            return true;
        }

        private void maybeReleaseConnection(Decoder.Result<?> result) {
            if (result.isOverflow() || result.isUnderflow()) {
                // Data remains to be processed
                return;
            }
            MessageHeader header
                = engine.responseDecoder().header().get();
            // Don't release if something follows
            if (header instanceof HttpResponse
                && ((HttpResponse) header).statusCode() % 100 == 1) {
                return;
            }
            if (engine.switchedTo().equals(Optional.of("websocket"))) {
                if (!result.closeConnection()) {
                    return;
                }
                // Is web socket close, inform application layer
                downPipeline.fire(new Closed<Void>(), this);
            }
            netConnChannel.setAssociated(WebAppMsgChannel.class, null);
            if (!result.closeConnection()) {
                // May be reused
                pooled.add(serverAddress, netConnChannel);
            }
            netConnChannel = null;
        }

        @SuppressWarnings("PMD.CommentRequired")
        public void handleClose(Close event) {
            if (engine.switchedTo().equals(Optional.of("websocket"))) {
                sendMessageUpstream(new WsCloseFrame(null, null),
                    netConnChannel);
            }
        }

        @SuppressWarnings("PMD.CommentRequired")
        public void handleClosed(Closed<?> event) {
            if (engine.switchedTo().equals(Optional.of("websocket"))) {
                downPipeline.fire(new Closed<Void>(), this);
            }
        }

    }

}
