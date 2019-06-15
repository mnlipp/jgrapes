/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2019 Michael N. Lipp
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

package org.jgrapes.net;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.HalfClosed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.OpenTcpConnection;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.Purge;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Connected;

/**
 * A component that receives and sends byte buffers on an
 * encrypted channel and sends and receives the corresponding
 * decrypted data on a plain channel.
 * 
 * The encrypted channel is assumed to be the network side
 * ("upstream") and therefore {@link Input} events represent
 * encrypted data and are decoded to {@link Output} events on
 * the plain channel ("downstream") and vice versa.
 */
@SuppressWarnings({ "PMD.ExcessiveImports" })
public class SslCodec extends Component {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(SslCodec.class.getName());

    private final Channel encryptedChannel;
    private final SSLContext sslContext;

    /**
     * Represents the encrypted channel in annotations.
     */
    private class EncryptedChannel extends ClassChannel {
    }

    /**
     * Creates a new codec that uses the given {@link SSLContext}.
     * 
     * @param plainChannel the component's channel
     * @param encryptedChannel the channel with the encrypted data
     * @param sslContext the SSL context to use
     */
    public SslCodec(Channel plainChannel, Channel encryptedChannel,
            SSLContext sslContext) {
        super(plainChannel, ChannelReplacements.create()
            .add(EncryptedChannel.class, encryptedChannel));
        this.encryptedChannel = encryptedChannel;
        this.sslContext = sslContext;
    }

    /**
     * Creates a new codec to be used as client.
     * 
     * @param plainChannel the component's channel
     * @param encryptedChannel the channel with the encrypted data
     * @param dontValidate if `true` accept all kinds of certificates
     */
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.CommentRequired",
        "PMD.ReturnEmptyArrayRatherThanNull", "PMD.UncommentedEmptyMethodBody",
        "PMD.AvoidDuplicateLiterals" })
    public SslCodec(Channel plainChannel, Channel encryptedChannel,
            boolean dontValidate) {
        super(plainChannel, ChannelReplacements.create()
            .add(EncryptedChannel.class, encryptedChannel));
        this.encryptedChannel = encryptedChannel;
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            if (dontValidate) {
                // Create a trust manager that does not validate certificate
                // chains
                final TrustManager[] trustAllCerts = {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(
                                X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(
                                X509Certificate[] certs, String authType) {
                        }
                    }
                };
                sslContext.init(null, trustAllCerts, null);
            } else {
                sslContext.init(null, null, null);
            }
            this.sslContext = sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates a new downstream connection as {@link LinkedIOSubchannel} 
     * of the network connection together with an {@link SSLEngine}.
     * 
     * @param event
     *            the accepted event
     */
    @Handler(channels = EncryptedChannel.class)
    public void onAccepted(Accepted event, IOSubchannel encryptedChannel) {
        new PlainChannel(event, encryptedChannel);
    }

    /**
     * Forward the connection request to the encrypted network.
     *
     * @param event the event
     */
    @Handler
    public void onOpenConnection(OpenTcpConnection event) {
        fire(new OpenTcpConnection(event.address()), encryptedChannel);
    }

    /**
     * Creates a new downstream connection as {@link LinkedIOSubchannel} 
     * of the network connection together with an {@link SSLEngine}.
     * 
     * @param event
     *            the accepted event
     */
    @Handler(channels = EncryptedChannel.class)
    public void onConnected(Connected event, IOSubchannel encryptedChannel) {
        new PlainChannel(event, encryptedChannel);
    }

    /**
     * Handles encrypted data from upstream (the network). The data is 
     * send through the {@link SSLEngine} and events are sent downstream 
     * (and in the initial phases upstream) according to the conversion 
     * results.
     * 
     * @param event the event
     * @param encryptedChannel the channel for exchanging the encrypted data
     * @throws InterruptedException 
     * @throws SSLException 
     * @throws ExecutionException 
     */
    @Handler(channels = EncryptedChannel.class)
    public void onInput(
            Input<ByteBuffer> event, IOSubchannel encryptedChannel)
            throws InterruptedException, SSLException, ExecutionException {
        @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
        final Optional<PlainChannel> plainChannel
            = (Optional<PlainChannel>) LinkedIOSubchannel
                .downstreamChannel(this, encryptedChannel);
        if (plainChannel.isPresent()) {
            plainChannel.get().sendDownstream(event);
        }
    }

    /**
     * Handles a half close event from the encrypted channel (client).
     * 
     * @param event the event
     * @param encryptedChannel the channel for exchanging the encrypted data
     * @throws InterruptedException 
     * @throws SSLException 
     */
    @Handler(channels = EncryptedChannel.class)
    public void onHalfClosed(HalfClosed event, IOSubchannel encryptedChannel)
            throws SSLException, InterruptedException {
        @SuppressWarnings("unchecked")
        final Optional<PlainChannel> plainChannel
            = (Optional<PlainChannel>) LinkedIOSubchannel
                .downstreamChannel(this, encryptedChannel);
        if (plainChannel.isPresent()) {
            plainChannel.get().upstreamHalfClosed();
        }
    }

    /**
     * Handles a close event from the encrypted channel (client).
     * 
     * @param event the event
     * @param encryptedChannel the channel for exchanging the encrypted data
     * @throws InterruptedException 
     * @throws SSLException 
     */
    @Handler(channels = EncryptedChannel.class)
    public void onClosed(Closed event, IOSubchannel encryptedChannel)
            throws SSLException, InterruptedException {
        @SuppressWarnings("unchecked")
        final Optional<PlainChannel> plainChannel
            = (Optional<PlainChannel>) LinkedIOSubchannel
                .downstreamChannel(this, encryptedChannel);
        if (plainChannel.isPresent()) {
            plainChannel.get().upstreamClosed();
        }
    }

    /**
     * Forwards a {@link Purge} event downstream.
     *
     * @param event the event
     * @param encryptedChannel the encrypted channel
     */
    @Handler(channels = EncryptedChannel.class)
    public void onPurge(Purge event, IOSubchannel encryptedChannel) {
        @SuppressWarnings("unchecked")
        final Optional<PlainChannel> plainChannel
            = (Optional<PlainChannel>) LinkedIOSubchannel
                .downstreamChannel(this, encryptedChannel);
        if (plainChannel.isPresent()) {
            plainChannel.get().purge();
        }
    }

    /**
     * Handles an {@link IOError} event from the encrypted channel (client)
     * by sending it downstream.
     * 
     * @param event the event
     * @param encryptedChannel the channel for exchanging the encrypted data
     * @throws InterruptedException 
     * @throws SSLException 
     */
    @Handler(channels = EncryptedChannel.class)
    public void onIOError(IOError event, IOSubchannel encryptedChannel)
            throws SSLException, InterruptedException {
        @SuppressWarnings("unchecked")
        final Optional<PlainChannel> plainChannel
            = (Optional<PlainChannel>) LinkedIOSubchannel
                .downstreamChannel(this, encryptedChannel);
        plainChannel.ifPresent(channel -> fire(new IOError(event), channel));
    }

    /**
     * Sends plain data through the engine and then upstream.
     * 
     * @param event
     *            the event with the data
     * @throws InterruptedException if the execution was interrupted
     * @throws SSLException if some SSL related problem occurs
     * @throws ExecutionException 
     */
    @Handler
    public void onOutput(Output<ByteBuffer> event,
            PlainChannel plainChannel)
            throws InterruptedException, SSLException, ExecutionException {
        if (plainChannel.hub() != this) {
            return;
        }
        plainChannel.sendUpstream(event);
    }

    /**
     * Forwards a close event upstream.
     * 
     * @param event
     *            the close event
     * @throws SSLException if an SSL related problem occurs
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler
    public void onClose(Close event, PlainChannel plainChannel)
            throws InterruptedException, SSLException {
        if (plainChannel.hub() != this) {
            return;
        }
        plainChannel.close(event);
    }

    /**
     * Represents the plain channel.
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private class PlainChannel extends LinkedIOSubchannel {
        public SocketAddress localAddress;
        public SocketAddress remoteAddress;
        public SSLEngine sslEngine;
        private EventPipeline downPipeline;
        private ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> downstreamPool;
        private ByteBuffer carryOver;
        private boolean[] inputProcessed = { false };

        /**
         * Instantiates a new plain channel from an accepted connection.
         *
         * @param event the event
         * @param upstreamChannel the upstream channel
         */
        public PlainChannel(Accepted event, IOSubchannel upstreamChannel) {
            super(SslCodec.this, channel(), upstreamChannel,
                newEventPipeline());
            localAddress = event.localAddress();
            remoteAddress = event.remoteAddress();
            init();
            sslEngine.setUseClientMode(false);
        }

        /**
         * Instantiates a new plain channel from an initiated connection.
         *
         * @param event the event
         * @param upstreamChannel the upstream channel
         */
        public PlainChannel(Connected event, IOSubchannel upstreamChannel) {
            super(SslCodec.this, channel(), upstreamChannel,
                newEventPipeline());
            localAddress = event.localAddress();
            remoteAddress = event.remoteAddress();
            init();
            sslEngine.setUseClientMode(true);

            // Forward downstream
            downPipeline.fire(
                new Connected(event.localAddress(), event.remoteAddress()),
                this);
        }

        private void init() {
            if (remoteAddress instanceof InetSocketAddress) {
                sslEngine = sslContext.createSSLEngine(
                    ((InetSocketAddress) remoteAddress).getAddress()
                        .getHostAddress(),
                    ((InetSocketAddress) remoteAddress).getPort());
            } else {
                sslEngine = sslContext.createSSLEngine();
            }
            String channelName = Components.objectName(SslCodec.this)
                + "." + Components.objectName(this);
            // Create buffer pools, adding 50 to decoded application buffer
            // size, see
            // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
            final int appBufSize
                = sslEngine.getSession().getApplicationBufferSize();
            downstreamPool = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> ByteBuffer.allocate(appBufSize + 50), 2)
                    .setName(channelName + ".downstream.buffers");
            // Provide buffers with application buffer size
            // for use by downstream components.
            setByteBufferPool(new ManagedBufferPool<>(ManagedBuffer::new,
                () -> ByteBuffer.allocate(appBufSize), 2)
                    .setName(channelName + ".upstream.buffers"));
            downPipeline = newEventPipeline();
            // Buffers for sending encrypted data upstream will be
            // obtained from upstream() and resized if required.
        }

        /**
         * Sends input downstream.
         *
         * @param event the event
         * @throws SSLException the SSL exception
         * @throws InterruptedException the interrupted exception
         * @throws ExecutionException the execution exception
         */
        public void sendDownstream(Input<ByteBuffer> event)
                throws SSLException, InterruptedException, ExecutionException {
            ByteBuffer input = event.buffer().duplicate();
            if (carryOver != null) {
                if (carryOver.remaining() < input.remaining()) {
                    // Shouldn't happen with carryOver having packet size
                    // bytes left, have seen it happen nevertheless.
                    carryOver.flip();
                    ByteBuffer extCarryOver = ByteBuffer.allocate(
                        carryOver.remaining() + input.remaining());
                    extCarryOver.put(carryOver);
                    carryOver = extCarryOver;
                }
                carryOver.put(input);
                carryOver.flip();
                input = carryOver;
                carryOver = null;
            }

            // Main processing
            processInput(input);

            // Check if data from incomplete packet remains in input buffer
            if (input.hasRemaining()) {
                // Actually, packet buffer size should be sufficient,
                // but since this is hard to test and doesn't really matter...
                carryOver = ByteBuffer.allocate(input.remaining()
                    + sslEngine.getSession().getPacketBufferSize() + 50);
                carryOver.put(input);
            }
        }

        @SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.NcssCount",
            "PMD.AvoidInstantiatingObjectsInLoops", "PMD.ExcessiveMethodLength",
            "PMD.NPathComplexity" })
        private SSLEngineResult processInput(ByteBuffer input)
                throws SSLException, InterruptedException, ExecutionException {
            SSLEngineResult unwrapResult;
            ManagedBuffer<ByteBuffer> unwrapped = downstreamPool.acquire();
            while (true) {
                unwrapResult
                    = sslEngine.unwrap(input, unwrapped.backingBuffer());
                synchronized (inputProcessed) {
                    inputProcessed[0] = true;
                    inputProcessed.notifyAll();
                }
                // Handle any handshaking procedures
                switch (unwrapResult.getHandshakeStatus()) {
                case NEED_TASK:
                    while (true) {
                        Runnable runnable = sslEngine.getDelegatedTask();
                        if (runnable == null) {
                            break;
                        }
                        // Having this handled by the response thread is
                        // probably not really necessary, but as the delegated
                        // task usually includes sending upstream...
                        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                        FutureTask<Boolean> task
                            = new FutureTask<>(runnable, true);
                        upstreamChannel().responsePipeline()
                            .executorService().submit(task);
                        task.get();
                    }
                    continue;

                case NEED_WRAP:
                    ManagedBuffer<ByteBuffer> feedback
                        = acquireUpstreamBuffer();
                    synchronized (sslEngine) {
                        SSLEngineResult wrapResult = sslEngine.wrap(
                            ManagedBuffer.EMPTY_BYTE_BUFFER.backingBuffer(),
                            feedback.backingBuffer());
                        upstreamChannel()
                            .respond(Output.fromSink(feedback, false));
                        if (wrapResult
                            .getHandshakeStatus() == HandshakeStatus.FINISHED) {
                            fireAccepted();
                        }
                    }
                    continue;

                case FINISHED:
                    fireAccepted();
                    break;

                default:
                    break;
                }

                // Anything to forward downstream?
                if (unwrapped.position() > 0) {
                    // forward unwrapped data
                    downPipeline.fire(Input.fromSink(unwrapped,
                        sslEngine.isInboundDone()), this);
                    unwrapped = null;
                }

                // If we have a buffer overflow or everything was okay
                // and there's data left, we try again, else we quit.
                if (unwrapResult.getStatus() != Status.BUFFER_OVERFLOW
                    && (unwrapResult.getStatus() != Status.OK
                        || !input.hasRemaining())) {
                    // Underflow or closed
                    if (unwrapped != null) {
                        unwrapped.unlockBuffer();
                    }
                    break;
                }

                // Make sure we have an output buffer.
                if (unwrapped == null) {
                    unwrapped = downstreamPool.acquire();
                }
            }
            return unwrapResult;
        }

        @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
        private void fireAccepted() {
            List<SNIServerName> snis = Collections.emptyList();
            if (sslEngine.getSession() instanceof ExtendedSSLSession) {
                snis = ((ExtendedSSLSession) sslEngine.getSession())
                    .getRequestedServerNames();
            }
            downPipeline.fire(new Accepted(
                localAddress, remoteAddress, true, snis), this);
        }

        /**
         * Send output upstream.
         *
         * @param event the event
         * @throws SSLException the SSL exception
         * @throws InterruptedException the interrupted exception
         * @throws ExecutionException 
         */
        public void sendUpstream(Output<ByteBuffer> event)
                throws SSLException, InterruptedException, ExecutionException {
            ByteBuffer output = event.buffer().backingBuffer().duplicate();
            processOutput(output, event.isEndOfRecord());
        }

        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.CyclomaticComplexity", "PMD.NcssCount",
            "PMD.NPathComplexity" })
        private void processOutput(ByteBuffer output, boolean eor)
                throws InterruptedException, SSLException, ExecutionException {
            ManagedBuffer<ByteBuffer> wrapped = acquireUpstreamBuffer();
            while (true) {
                SSLEngineResult wrapResult;
                // Cheap synchronization: no (relevant) input
                inputProcessed[0] = false;
                synchronized (sslEngine) {
                    wrapResult
                        = sslEngine.wrap(output, wrapped.backingBuffer());
                    // Anything to be sent upstream?
                    if (wrapped.position() > 0) {
                        upstreamChannel().respond(Output.fromSink(wrapped,
                            sslEngine.isInboundDone()
                                || eor && !output.hasRemaining()));
                        wrapped = null;
                    }
                }
                switch (wrapResult.getHandshakeStatus()) {
                case NEED_TASK:
                    while (true) {
                        Runnable runnable = sslEngine.getDelegatedTask();
                        if (runnable == null) {
                            break;
                        }
                        runnable.run();
                    }
                    continue;

                case NEED_UNWRAP:
                    // Input required. Wait until
                    // input becomes available and retry.
                    synchronized (inputProcessed) {
                        while (!inputProcessed[0]) {
                            inputProcessed.wait();
                        }
                    }
                    break;

                default:
                    break;
                }

                // If we have a buffer overflow or everything was okay
                // and there's data left, we try again, else we quit.
                if (wrapResult.getStatus() != Status.BUFFER_OVERFLOW
                    && (wrapResult.getStatus() != Status.OK
                        || !output.hasRemaining())) {
                    // Underflow or closed
                    if (wrapped != null) {
                        wrapped.unlockBuffer();
                    }
                    // Warn if data is discarded
                    if (output.hasRemaining()) {
                        logger.warning(() -> toString()
                            + ": Upstream data discarded, SSLEngine status: "
                            + wrapResult.getStatus());
                    }
                    break;
                }

                // Was handshake (or partial content), get new buffer and try
                // again
                if (wrapped == null) {
                    wrapped = acquireUpstreamBuffer();
                }
            }
        }

        /**
         * Close the connection.
         *
         * @param event the event
         * @throws InterruptedException the interrupted exception
         * @throws SSLException the SSL exception
         */
        public void close(Close event)
                throws InterruptedException, SSLException {
            sslEngine.closeOutbound();
            while (!sslEngine.isOutboundDone()) {
                ManagedBuffer<ByteBuffer> feedback = acquireUpstreamBuffer();
                sslEngine.wrap(ManagedBuffer.EMPTY_BYTE_BUFFER
                    .backingBuffer(), feedback.backingBuffer());
                upstreamChannel().respond(Output.fromSink(feedback, false));
            }
            upstreamChannel().respond(new Close());
        }

        /**
         * Handles the {@link HalfClosed} event.
         *
         * @throws SSLException the SSL exception
         * @throws InterruptedException the interrupted exception
         */
        public void upstreamHalfClosed()
                throws SSLException, InterruptedException {
            if (sslEngine.isInboundDone()) {
                // Was properly closed on SSL level
                return;
            }
            try {
                sslEngine.closeInbound();
                sslEngine.closeOutbound();
                while (!sslEngine.isOutboundDone()) {
                    ManagedBuffer<ByteBuffer> feedback
                        = acquireUpstreamBuffer();
                    SSLEngineResult result = sslEngine.wrap(
                        ManagedBuffer.EMPTY_BYTE_BUFFER.backingBuffer(),
                        feedback.backingBuffer());
                    // This is required for/since JDK 11. It claims that
                    // outbound is not done, but doesn't produce any
                    // additional
                    // data.
                    if (result.getStatus() == Status.CLOSED
                        || feedback.position() == 0) {
                        feedback.unlockBuffer();
                        break;
                    }
                    upstreamChannel()
                        .respond(Output.fromSink(feedback, false));
                }
            } catch (SSLException e) {
                // Several clients (notably chromium, see
                // https://bugs.chromium.org/p/chromium/issues/detail?id=118366
                // don't close the connection properly. So nobody is really
                // interested in this message
                logger.log(Level.FINEST, e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }

        /**
         * Forwards the {@link Closed} event downstream.
         *
         * @throws SSLException the SSL exception
         * @throws InterruptedException the interrupted exception
         */
        public void upstreamClosed()
                throws SSLException, InterruptedException {
            downPipeline.fire(new Closed(), this);
        }

        private ManagedBuffer<ByteBuffer> acquireUpstreamBuffer()
                throws InterruptedException {
            ManagedBuffer<ByteBuffer> feedback
                = upstreamChannel().byteBufferPool().acquire();
            int encSize
                = sslEngine.getSession().getPacketBufferSize() + 50;
            if (feedback.capacity() < encSize) {
                feedback.replaceBackingBuffer(ByteBuffer.allocate(
                    encSize));
            }
            return feedback;
        }

        /**
         * Fire a {@link Purge} event downstream.
         */
        public void purge() {
            downPipeline.fire(new Purge(), this);
        }
    }
}
