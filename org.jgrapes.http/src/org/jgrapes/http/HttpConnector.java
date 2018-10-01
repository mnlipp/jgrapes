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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jdrupes.httpcodec.ClientEngine;
import org.jdrupes.httpcodec.Codec;
import org.jdrupes.httpcodec.Decoder;
import org.jdrupes.httpcodec.MessageHeader;
import org.jdrupes.httpcodec.ProtocolException;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.protocols.http.HttpResponse;
import org.jdrupes.httpcodec.protocols.http.client.HttpRequestEncoder;
import org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder;
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
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.OpenTcpConnection;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.net.TcpChannel;
import org.jgrapes.net.events.Connected;

/**
 * A converter component that receives and sends web application
 * layer messages on {@link IOSubchannel}s of its channel and
 * byte buffers on associated network channels.
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class HttpConnector extends Component {

    private int applicationBufferSize = -1;
    private Channel netMainChannel;
    private Map<SocketAddress, Set<WebAppMsgChannel>> connecting
        = new HashMap<>();
    private PoolingIndex<SocketAddress, TcpChannel> pooled
        = new PoolingIndex<>();

    /**
     * Denotes the network channel in handler annotations.
     */
    private static class NetworkChannel extends ClassChannel {
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

    @Handler
    public void onRequest(Request.Out event)
            throws InterruptedException, IOException {
        new WebAppMsgChannel(event);
    }

    @Handler
    public void onOutput(Output<?> event, WebAppMsgChannel appChannel)
            throws InterruptedException {
        appChannel.appOutput(event);
    }

    @Handler(channels = NetworkChannel.class)
    public void onConnected(Connected event, TcpChannel netConnChannel)
            throws InterruptedException, IOException {
        // Check if an app channel has been waiting for such a connection
        WebAppMsgChannel[] appChannel = { null };
        synchronized (connecting) {
            connecting.computeIfPresent(event.remoteAddress(), (key, set) -> {
                Iterator<WebAppMsgChannel> iter = set.iterator();
                appChannel[0] = iter.next();
                iter.remove();
                return set.isEmpty() ? null : set;
            });
        }
        if (appChannel[0] != null) {
            appChannel[0].connected(netConnChannel);
        }
    }

    @Handler(channels = NetworkChannel.class)
    public void onIoError(IOError event) throws IOException {
        for (Channel channel : event.channels()) {
            if (channel instanceof TcpChannel) {
                // Error while using established network connection
                TcpChannel netConnChannel = (TcpChannel) channel;
                Optional<WebAppMsgChannel> appChannel
                    = netConnChannel.associated(WebAppMsgChannel.class);
                if (appChannel.isPresent()) {
                    // Error while using a network connection
                    appChannel.get().ioError(event, netConnChannel);
                    continue;
                }
                // Just in case...
                pooled.remove(netConnChannel.remoteAddress(), netConnChannel);
                continue;
            }
            // Error while trying to establish the network connection
            if (event.event() instanceof OpenTcpConnection) {
                OpenTcpConnection connEvent
                    = (OpenTcpConnection) event.event();
                Optional<Set<WebAppMsgChannel>> erroneous;
                synchronized (connecting) {
                    erroneous = Optional
                        .ofNullable(connecting.get(connEvent.address()));
                    connecting.remove(connEvent.address());
                }
                erroneous.ifPresent(set -> {
                    for (WebAppMsgChannel chann : set) {
                        chann.openError(event);
                    }
                });
            }
        }
    }

    @Handler(channels = NetworkChannel.class)
    public void onInput(Input<ByteBuffer> event, TcpChannel netConnChannel)
            throws InterruptedException, ProtocolException {
        Optional<WebAppMsgChannel> appChannel
            = netConnChannel.associated(WebAppMsgChannel.class);
        if (appChannel.isPresent()) {
            appChannel.get().netInput(event, netConnChannel);
        }
    }

    @Handler(channels = NetworkChannel.class)
    public void onClosed(Input<ByteBuffer> event, TcpChannel netConnChannel)
            throws IOException {
        pooled.remove(netConnChannel.remoteAddress(), netConnChannel);
    }

    /**
     * An application layer channel. When an object is created, it is first
     * inserted into the {@link HttpConnector#connecting} map. Once a network
     * channel has been assigned to it, it is primarily referenced by that 
     * network channel. 
     */
    private class WebAppMsgChannel extends DefaultSubchannel {
        // Starts as ClientEngine<HttpRequest,HttpResponse> but may change
        private ClientEngine<HttpRequest, HttpResponse> engine
            = new ClientEngine<>(new HttpRequestEncoder(),
                new HttpResponseDecoder());
        private InetSocketAddress serverAddress;
        private Request.Out request;
        private ManagedBuffer<ByteBuffer> outBuffer;
        private ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> byteBufferPool;
        private ManagedBufferPool<ManagedBuffer<CharBuffer>,
                CharBuffer> charBufferPool;
        private ManagedBufferPool<?, ?> currentPool;
        private TcpChannel netConnChannel;
        private EventPipeline downPipeline;

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
            serverAddress = new InetSocketAddress(
                event.requestUri().getHost(), event.requestUri().getPort());
            if (serverAddress.isUnresolved()) {
                downPipeline.fire(
                    new HostUnresolved(event, "Host cannot be resolved."),
                    this);
                return;
            }

            // Re-use TCP connection, if possible
            TcpChannel recycled = pooled.poll(serverAddress);
            if (recycled != null) {
                connected(recycled);
                return;
            }
            synchronized (connecting) {
                connecting
                    .computeIfAbsent(serverAddress, key -> new HashSet<>())
                    .add(this);
            }

            // Fire on main network channel (targeting the tcp connector)
            // as a follow up event (using the current pipeline).
            fire(new OpenTcpConnection(serverAddress), netMainChannel);

            // TODO: timeout
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
        public void ioError(IOError event, TcpChannel netConnChannel) {
            // TODO
            downPipeline.fire(IOError.duplicate(event), this);
        }

        public void connected(TcpChannel netConnChannel)
                throws InterruptedException, IOException {
            // Associate the network channel with this application channel
            this.netConnChannel = netConnChannel;
            netConnChannel.setAssociated(WebAppMsgChannel.class, this);

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

            // Now send request as if it came from downstream (to
            // avoid confusion with output events that may be
            // generated in parallel, see below).
            responsePipeline().submit(new Callable<Void>() {

                public Void call() throws InterruptedException {
                    engine.encode(request.httpRequest());
                    boolean hasBody = request.httpRequest().hasPayload();
                    while (true) {
                        outBuffer = netConnChannel.byteBufferPool().acquire();
                        Codec.Result result
                            = engine.encode(Codec.EMPTY_IN,
                                outBuffer.backingBuffer(), !hasBody);
                        if (result.isOverflow()) {
                            netConnChannel
                                .respond(Output.fromSink(outBuffer, false));
                            continue;
                        }
                        if (hasBody) {
                            // Keep buffer with incomplete request to be further
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

            // Forward Connected event downstream to e.g. start preparation
            // of output events for payload data.
            downPipeline.fire(new HttpConnected(request,
                netConnChannel.localAddress(), netConnChannel.remoteAddress()),
                this);
        }

        public void appOutput(Output<?> event) throws InterruptedException {
            Buffer eventData = event.data();
            Buffer input;
            if (eventData instanceof ByteBuffer) {
                input = ((ByteBuffer) eventData).duplicate();
            } else if (eventData instanceof CharBuffer) {
                input = ((CharBuffer) eventData).duplicate();
            } else {
                return;
            }
//            if (upgradedTo == UpgradedState.WEB_SOCKET
//                && currentWsMessage == null) {
//                // When switched to WebSockets, we only have Input and Output
//                // events. Add header automatically.
//                @SuppressWarnings("unchecked")
//                ServerEngine<?, MessageHeader> wsEngine
//                    = (ServerEngine<?, MessageHeader>) engine;
//                currentWsMessage = new WsMessageHeader(
//                    event.buffer().backingBuffer() instanceof CharBuffer,
//                    true);
//                wsEngine.encode(currentWsMessage);
//            }
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
//            if (upgradedTo == UpgradedState.WEB_SOCKET
//                && event.isEndOfRecord()) {
//                currentWsMessage = null;
//            }
        }

        public void netInput(Input<ByteBuffer> event, TcpChannel channel)
                throws InterruptedException, ProtocolException {
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
                if (result.isHeaderCompleted()) {
                    HttpResponse header
                        = engine.responseDecoder().header().get();
                    if (!handleResponseHeader(header)) {
                        break;
                    }
                    if (!header.hasPayload()) {
                        releaseConnection(serverAddress, channel);
                    }
                }
                if (bodyData != null) {
                    if (bodyData.position() > 0) {
                        boolean eor
                            = !result.isOverflow() && !result.isUnderflow();
                        respond(Input.fromSink(bodyData, eor));
                        if (eor) {
                            releaseConnection(serverAddress, channel);
                        }
                    } else {
                        bodyData.unlockBuffer();
                    }
                    bodyData = null;
                }
                wasOverflow = result.isOverflow();
            }
        }

        private boolean handleResponseHeader(MessageHeader response) {
            if (response instanceof HttpResponse) {
                HttpResponse httpResponse = (HttpResponse) response;
                if (httpResponse.hasPayload()) {
                    if (httpResponse.findValue(
                        HttpField.CONTENT_TYPE, Converters.MEDIA_TYPE)
                        .map(type -> type.value().topLevelType()
                            .equalsIgnoreCase("text"))
                        .orElse(false)) {
                        currentPool = charBufferPool;
                    } else {
                        currentPool = byteBufferPool;
                    }
                }
                downPipeline.fire(new Response(httpResponse), this);
//            } else if (request instanceof WsMessageHeader) {
//                WsMessageHeader wsMessage = (WsMessageHeader) request;
//                if (wsMessage.hasPayload()) {
//                    if (wsMessage.isTextMode()) {
//                        currentPool = charBufferPool;
//                    } else {
//                        currentPool = byteBufferPool;
//                    }
//                }
            }
            return true;
        }

        private void releaseConnection(SocketAddress address,
                TcpChannel netConnChannel) {
            netConnChannel.setAssociated(WebAppMsgChannel.class, null);
            pooled.add(address, netConnChannel);
            this.netConnChannel = null;
        }
    }

}
