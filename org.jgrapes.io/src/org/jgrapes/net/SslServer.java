/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
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

import org.jgrapes.core.Channel;
import org.jgrapes.core.ClassChannel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.annotation.HandlerDefinition.ChannelReplacements;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.events.Purge;
import org.jgrapes.io.util.LinkedIOSubchannel;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.net.events.Accepted;

/**
 * A component that receives and send byte buffers on an
 * encrypted channel and sends and receives the corresponding
 * decrypted data on its own channel.
 */
@SuppressWarnings("PMD.ExcessiveImports")
public class SslServer extends Component {

    @SuppressWarnings("PMD.VariableNamingConventions")
    private static final Logger logger
        = Logger.getLogger(SslServer.class.getName());

    private final SSLContext sslContext;

    /**
     * Represents the encrypted channel in annotations.
     */
    private class EncryptedChannel extends ClassChannel {
    }

    /**
     * @param plainChannel the component's channel
     * @param encryptedChannel the channel with the encrypted data
     * @param sslContext the SSL context to use
     */
    public SslServer(Channel plainChannel, Channel encryptedChannel,
            SSLContext sslContext) {
        super(plainChannel, ChannelReplacements.create()
            .add(EncryptedChannel.class, encryptedChannel));
        this.sslContext = sslContext;
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
     * Sends decrypted data through the engine and then upstream.
     * 
     * @param event
     *            the event with the data
     * @throws InterruptedException if the execution was interrupted
     * @throws SSLException if some SSL related problem occurs
     */
    @Handler
    public void onOutput(Output<ByteBuffer> event,
            PlainChannel plainChannel)
            throws InterruptedException, SSLException {
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
    private class PlainChannel extends LinkedIOSubchannel {
        public SocketAddress localAddress;
        public SocketAddress remoteAddress;
        public SSLEngine sslEngine;
        private final ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> downstreamPool;
        private boolean isInputClosed;
        private ByteBuffer carryOver;

        /**
         * Instantiates a new plain channel.
         *
         * @param event the event
         * @param upstreamChannel the upstream channel
         */
        public PlainChannel(Accepted event, IOSubchannel upstreamChannel) {
            super(SslServer.this, channel(), upstreamChannel,
                newEventPipeline());
            localAddress = event.localAddress();
            remoteAddress = event.remoteAddress();
            if (remoteAddress instanceof InetSocketAddress) {
                sslEngine = sslContext.createSSLEngine(
                    ((InetSocketAddress) remoteAddress).getAddress()
                        .getHostAddress(),
                    ((InetSocketAddress) remoteAddress).getPort());
            } else {
                sslEngine = sslContext.createSSLEngine();
            }
            sslEngine.setUseClientMode(false);

            String channelName = Components.objectName(SslServer.this)
                + "." + Components.objectName(this);
            // Create buffer pools, adding 50 to application buffer size, see
            // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
            int decBufSize = sslEngine.getSession()
                .getApplicationBufferSize() + 50;
            downstreamPool = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocate(decBufSize);
                }, 2)
                    .setName(channelName + ".downstream.buffers");
            int encBufSize = sslEngine.getSession().getPacketBufferSize();
            setByteBufferPool(new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocate(encBufSize);
                }, 2)
                    .setName(channelName + ".upstream.buffers"));
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
            ManagedBuffer<ByteBuffer> unwrapped = downstreamPool.acquire();
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
            SSLEngineResult processingResult = processInput(unwrapped, input);

            // final message?
            if (processingResult.getStatus() == Status.CLOSED
                && !isInputClosed) {
                Closed evt = new Closed();
                newEventPipeline().fire(evt, this);
                evt.get();
                isInputClosed = true;
                return;
            }

            // Check if data from incomplete packet remains in input buffer
            if (input.hasRemaining()) {
                // Actually, packet buffer size should be sufficient,
                // but since this is hard to test and doesn't really matter...
                carryOver = ByteBuffer.allocate(input.remaining()
                    + sslEngine.getSession().getPacketBufferSize() + 50);
                carryOver.put(input);
            }
        }

        private SSLEngineResult processInput(
                ManagedBuffer<ByteBuffer> unwrapped, ByteBuffer input)
                throws SSLException, InterruptedException, ExecutionException {
            SSLEngineResult unwrapResult;
            while (true) {
                unwrapResult = sslEngine.unwrap(
                    input, unwrapped.backingBuffer());
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
                        = upstreamChannel().byteBufferPool().acquire();
                    SSLEngineResult wrapResult = sslEngine.wrap(
                        ManagedBuffer.EMPTY_BYTE_BUFFER
                            .backingBuffer(),
                        feedback.backingBuffer());
                    upstreamChannel().respond(Output.fromSink(feedback, false));
                    if (wrapResult
                        .getHandshakeStatus() == HandshakeStatus.FINISHED) {
                        fireAccepted();
                    }
                    continue;

                case FINISHED:
                    fireAccepted();
                    // fall through
                case NEED_UNWRAP:
                    // sslEngine.unwrap sometimes return NEED_UNWRAP in
                    // combination with CLOSED, though this doesn't really
                    // make sense.
                    if (unwrapResult.getStatus() != Status.BUFFER_UNDERFLOW
                        && unwrapResult.getStatus() != Status.CLOSED) {
                        continue;
                    }
                    break;

                default:
                    break;
                }

                // Just to make sure... (Initial allocation should be
                // big enough.)
                if (unwrapResult.getStatus() == Status.BUFFER_OVERFLOW) {
                    if (unwrapped.position() > 0) {
                        // forward data received up to now
                        fire(Input.fromSink(unwrapped,
                            sslEngine.isInboundDone()), this);
                    }
                    unwrapped = downstreamPool.acquire();
                    if (unwrapped.capacity() < sslEngine.getSession()
                        .getApplicationBufferSize() + 50) {
                        unwrapped.replaceBackingBuffer(ByteBuffer.allocate(
                            sslEngine.getSession()
                                .getApplicationBufferSize() + 50));
                    }
                    continue;
                }

                // If we get here, handshake has completed or no input is left
                if (unwrapResult.getStatus() != Status.OK) {
                    // Underflow, overflow or closed
                    break;
                }
            }

            if (unwrapped.position() == 0) {
                // Was only handshake
                unwrapped.unlockBuffer();
            } else {
                // forward data received
                fire(Input.fromSink(unwrapped, sslEngine.isInboundDone()),
                    this);
            }
            return unwrapResult;
        }

        /**
         * Send output upstream.
         *
         * @param event the event
         * @throws SSLException the SSL exception
         * @throws InterruptedException the interrupted exception
         */
        public void sendUpstream(Output<ByteBuffer> event)
                throws SSLException, InterruptedException {
            ByteBuffer output = event.buffer().backingBuffer().duplicate();
            while (output.hasRemaining() && !sslEngine.isInboundDone()) {
                ManagedBuffer<ByteBuffer> out
                    = upstreamChannel().byteBufferPool().acquire();
                sslEngine.wrap(output, out.backingBuffer());
                upstreamChannel().respond(
                    Output.fromSink(out, event.isEndOfRecord()
                        && !output.hasRemaining()));
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
                ManagedBuffer<ByteBuffer> feedback
                    = upstreamChannel().byteBufferPool().acquire();
                sslEngine.wrap(ManagedBuffer.EMPTY_BYTE_BUFFER
                    .backingBuffer(), feedback.backingBuffer());
                upstreamChannel().respond(Output.fromSink(feedback, false));
            }
            upstreamChannel().respond(new Close());
        }

        @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
        private void fireAccepted() {
            List<SNIServerName> snis = Collections.emptyList();
            if (sslEngine.getSession() instanceof ExtendedSSLSession) {
                snis = ((ExtendedSSLSession) sslEngine.getSession())
                    .getRequestedServerNames();
            }
            fire(new Accepted(
                localAddress, remoteAddress, true, snis), this);
        }

        /**
         * Forwards the {@link Closed} event downstream.
         *
         * @throws SSLException the SSL exception
         * @throws InterruptedException the interrupted exception
         */
        public void upstreamClosed()
                throws SSLException, InterruptedException {
            if (!isInputClosed) {
                // was not properly closed on SSL layer
                Closed evt = new Closed();
                newEventPipeline().fire(evt, this);
                evt.get();
            }
            try {
                sslEngine.closeInbound();
                while (!sslEngine.isOutboundDone()) {
                    ManagedBuffer<ByteBuffer> feedback
                        = upstreamChannel().byteBufferPool().acquire();
                    sslEngine.wrap(ManagedBuffer.EMPTY_BYTE_BUFFER
                        .backingBuffer(), feedback.backingBuffer());
                    upstreamChannel().respond(Output.fromSink(feedback, false));
                }
            } catch (SSLException e) {
                // Several clients (notably chromium, see
                // https://bugs.chromium.org/p/chromium/issues/detail?id=118366
                // don't close the connection properly. So nobody is really
                // interested in this message
                logger.log(Level.FINEST, e.getMessage(), e);
            }
        }

        /**
         * Fire a {@link Purge} event downstream.
         */
        public void purge() {
            fire(new Purge(), this);
        }
    }
}
