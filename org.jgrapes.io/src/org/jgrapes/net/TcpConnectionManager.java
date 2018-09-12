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

package org.jgrapes.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.EventPipeline;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.IOSubchannel.DefaultSubchannel;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.HalfClosed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.NioRegistration.Registration;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;

/**
 * Provides a base class for the {@link TcpServer} and the {@link TcpConnector}.
 */
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.ExcessivePublicCount",
    "PMD.NcssCount", "PMD.EmptyCatchBlock", "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveClassLength" })
public abstract class TcpConnectionManager extends Component {

    private int bufferSize;
    protected final Set<TcpChannel> channels = new HashSet<>();
    private ExecutorService executorService;

    /**
     * Creates a new server using the given channel.
     * 
     * @param componentChannel the component's channel
     */
    public TcpConnectionManager(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Sets the buffer size for the send an receive buffers.
     * If no size is set, the system defaults will be used.
     * 
     * @param bufferSize the size to use for the send and receive buffers
     * @return the TCP connection manager for easy chaining
     */
    public TcpConnectionManager setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Return the configured buffer size.
     *
     * @return the bufferSize
     */
    public int bufferSize() {
        return bufferSize;
    }

    /**
     * Sets an executor service to be used by the event pipelines
     * that process the data from the network. Setting this
     * to an executor service with a limited number of threads
     * allows to control the maximum load from the network.
     * 
     * @param executorService the executorService to set
     * @return the TCP connection manager for easy chaining
     * @see Manager#newEventPipeline(ExecutorService)
     */
    public TcpConnectionManager
            setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Returns the executor service.
     *
     * @return the executorService
     */
    public ExecutorService executorService() {
        return executorService;
    }

    /**
     * Writes the data passed in the event. 
     * 
     * The end of record flag is used to determine if a channel is 
     * eligible for purging. If the flag is set and all output has 
     * been processed, the channel is purgeable until input is 
     * received or another output event causes the state to be 
     * reevaluated. 
     *
     * @param event the event
     * @param channel the channel
     * @throws InterruptedException the interrupted exception
     */
    @Handler
    public void onOutput(Output<ByteBuffer> event,
            TcpChannel channel) throws InterruptedException {
        if (channels.contains(channel)) {
            channel.write(event);
        }
    }

    /**
     * Removes the channel from the set of registered channels.
     *
     * @param channel the channel
     * @return true, if channel was registered
     */
    protected boolean removeChannel(TcpChannel channel) {
        synchronized (channels) {
            return channels.remove(channel);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Components.objectName(this);
    }

    /**
     * The close state.
     */
    private enum ConnectionState {
        OPEN, DELAYED_EVENT, DELAYED_REQUEST, HALF_CLOSED, CLOSED
    }

    /**
     * The purgeable state.
     */
    private enum PurgeableState {
        NO, PENDING, YES
    }

    /**
     * The internal representation of a connection. 
     */
    protected class TcpChannel
            extends DefaultSubchannel implements NioHandler {

        private final SocketChannel nioChannel;
        private EventPipeline downPipeline;
        private final ManagedBufferPool<ManagedBuffer<ByteBuffer>,
                ByteBuffer> readBuffers;
        private Registration registration;
        private final Queue<
                ManagedBuffer<ByteBuffer>.ByteBufferView> pendingWrites
                    = new ArrayDeque<>();
        private ConnectionState connState = ConnectionState.OPEN;
        private PurgeableState purgeable = PurgeableState.NO;
        private long becamePurgeableAt;

        /**
         * @param nioChannel the channel
         * @throws IOException if an I/O error occured
         */
        public TcpChannel(SocketChannel nioChannel) throws IOException {
            super(channel(), newEventPipeline());
            this.nioChannel = nioChannel;
            if (executorService == null) {
                downPipeline = newEventPipeline();
            } else {
                downPipeline = newEventPipeline(executorService);
            }

            String channelName
                = Components.objectName(TcpConnectionManager.this)
                    + "." + Components.objectName(this);
            int writeBufferSize = bufferSize == 0
                ? nioChannel.socket().getSendBufferSize()
                : bufferSize;
            setByteBufferPool(new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocate(writeBufferSize);
                }, 2)
                    .setName(channelName + ".upstream.buffers"));

            int readBufferSize = bufferSize == 0
                ? nioChannel.socket().getReceiveBufferSize()
                : bufferSize;
            readBuffers = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> {
                    return ByteBuffer.allocate(readBufferSize);
                }, 2)
                    .setName(channelName + ".downstream.buffers");

            // Register with dispatcher
            nioChannel.configureBlocking(false);
            TcpConnectionManager.this.fire(
                new NioRegistration(this, nioChannel, 0,
                    TcpConnectionManager.this),
                Channel.BROADCAST);
        }

        /**
         * Gets the nio channel.
         *
         * @return the nioChannel
         */
        public SocketChannel nioChannel() {
            return nioChannel;
        }

        /**
         * Gets the read buffers.
         *
         * @return the readBuffers
         */
        public ManagedBufferPool<ManagedBuffer<ByteBuffer>, ByteBuffer>
                readBuffers() {
            return readBuffers;
        }

        /**
         * Gets the down pipeline.
         *
         * @return the downPipeline
         */
        public EventPipeline downPipeline() {
            return downPipeline;
        }

        /**
         * Invoked when registration has completed.
         * 
         * @param event the completed event
         * @throws InterruptedException if the execution was interrupted
         * @throws IOException if an I/O error occurred
         */
        public void registrationComplete(NioRegistration event)
                throws InterruptedException, IOException {
            registration = event.get();
            registration.updateInterested(SelectionKey.OP_READ);
        }

        /**
         * Checks if is purgeable.
         *
         * @return true, if is purgeable
         */
        public boolean isPurgeable() {
            return purgeable == PurgeableState.YES;
        }

        /**
         * Gets the the time when the connection became purgeable.
         *
         * @return the time
         */
        public long becamePurgeableAt() {
            return becamePurgeableAt;
        }

        /**
         * Write the data on this channel.
         * 
         * @param event the event
         */
        public void write(Output<ByteBuffer> event)
                throws InterruptedException {
            synchronized (pendingWrites) {
                if (!nioChannel.isOpen()) {
                    return;
                }
                ManagedBuffer<ByteBuffer>.ByteBufferView reader
                    = event.buffer().newByteBufferView();
                if (!pendingWrites.isEmpty()) {
                    reader.managedBuffer().lockBuffer();
                    purgeable = event.isEndOfRecord() ? PurgeableState.PENDING
                        : PurgeableState.NO;
                    pendingWrites.add(reader);
                    return;
                }
                try {
                    nioChannel.write(reader.get());
                } catch (IOException e) {
                    forceClose(e);
                    return;
                }
                if (!reader.get().hasRemaining()) {
                    if (event.isEndOfRecord()) {
                        becamePurgeableAt = System.currentTimeMillis();
                        purgeable = PurgeableState.YES;
                    } else {
                        purgeable = PurgeableState.NO;
                    }
                    return;
                }
                reader.managedBuffer().lockBuffer();
                purgeable = event.isEndOfRecord() ? PurgeableState.PENDING
                    : PurgeableState.NO;
                pendingWrites.add(reader);
                registration.updateInterested(
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }

        @Override
        public void handleOps(int ops) throws InterruptedException {
            if ((ops & SelectionKey.OP_READ) != 0) {
                handleReadOp();
            }
            if ((ops & SelectionKey.OP_WRITE) != 0) {
                handleWriteOp();
            }
        }

        /**
         * Gets a buffer from the pool and reads available data into it.
         * Sends the result as event. 
         * 
         * @throws InterruptedException
         * @throws IOException
         */
        @SuppressWarnings("PMD.EmptyCatchBlock")
        private void handleReadOp() throws InterruptedException {
            ManagedBuffer<ByteBuffer> buffer;
            buffer = readBuffers.acquire();
            try {
                int bytes = buffer.fillFromChannel(nioChannel);
                if (bytes == 0) {
                    buffer.unlockBuffer();
                    return;
                }
                if (bytes > 0) {
                    purgeable = PurgeableState.NO;
                    downPipeline.fire(Input.fromSink(buffer, false), this);
                    return;
                }
            } catch (IOException e) {
                // Buffer already unlocked by fillFromChannel
                forceClose(e);
                return;
            }
            // EOF (-1) from other end
            buffer.unlockBuffer();
            synchronized (nioChannel) {
                if (connState == ConnectionState.HALF_CLOSED) {
                    // Other end confirms our close, complete close
                    try {
                        nioChannel.close();
                    } catch (IOException e) {
                        // Ignored for close
                    }
                    connState = ConnectionState.CLOSED;
                    downPipeline.fire(new Closed(), this);
                    return;
                }

            }
            // Other end initiates close
            downPipeline.fire(new HalfClosed(), this);
            downPipeline.submit(() -> {
                removeChannel(this);
                downPipeline.fire(new Closed(), this);
                synchronized (pendingWrites) {
                    synchronized (nioChannel) {
                        try {
                            if (!pendingWrites.isEmpty()) {
                                // Pending writes, delay close
                                connState = ConnectionState.DELAYED_REQUEST;
                                return;
                            }
                            // Nothing left to do, close
                            nioChannel.close();
                            connState = ConnectionState.CLOSED;
                        } catch (IOException e) {
                            // Ignored for close
                        }
                    }
                }
            });
        }

        /**
         * Checks if there is still data to be written. This may be
         * a left over in an incompletely written buffer or a complete
         * pending buffer. 
         * 
         * @throws IOException
         * @throws InterruptedException 
         */
        @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis",
            "PMD.EmptyCatchBlock",
            "PMD.AvoidBranchingStatementAsLastInLoop" })
        private void handleWriteOp() throws InterruptedException {
            while (true) {
                ManagedBuffer<ByteBuffer>.ByteBufferView head = null;
                synchronized (pendingWrites) {
                    if (pendingWrites.isEmpty()) {
                        // Nothing left to write, stop getting ops
                        registration.updateInterested(SelectionKey.OP_READ);
                        // Was the connection closed while we were writing?
                        if (connState == ConnectionState.DELAYED_REQUEST
                            || connState == ConnectionState.DELAYED_EVENT) {
                            synchronized (nioChannel) {
                                try {
                                    if (connState == ConnectionState.DELAYED_REQUEST) {
                                        // Delayed close request from other end,
                                        // complete
                                        nioChannel.close();
                                        connState = ConnectionState.CLOSED;
                                    }
                                    if (connState == ConnectionState.DELAYED_EVENT) {
                                        // Delayed close from this end, initiate
                                        nioChannel.shutdownOutput();
                                        connState = ConnectionState.HALF_CLOSED;
                                    }
                                } catch (IOException e) {
                                    // Ignored for close
                                }
                            }
                        } else {
                            if (purgeable == PurgeableState.PENDING) {
                                purgeable = PurgeableState.YES;
                            }
                        }
                        break; // Nothing left to do
                    }
                    head = pendingWrites.peek();
                    if (!head.get().hasRemaining()) {
                        // Nothing left in head buffer, try next
                        head.managedBuffer().unlockBuffer();
                        pendingWrites.remove();
                        continue;
                    }
                }
                try {
                    nioChannel.write(head.get()); // write...
                } catch (IOException e) {
                    forceClose(e);
                    return;
                }
                break; // ... and wait for next op
            }
        }

        /**
         * Closes this channel.
         * 
         * @throws IOException if an error occurs
         * @throws InterruptedException if the execution was interrupted 
         */
        public void close() throws IOException, InterruptedException {
            if (!removeChannel(this)) {
                return;
            }
            synchronized (pendingWrites) {
                if (!pendingWrites.isEmpty()) {
                    // Pending writes, delay close until done
                    connState = ConnectionState.DELAYED_EVENT;
                    return;
                }
                // Nothing left to do, proceed
                synchronized (nioChannel) {
                    if (nioChannel.isOpen()) {
                        // Initiate close, must be confirmed by other end
                        nioChannel.shutdownOutput();
                        connState = ConnectionState.HALF_CLOSED;
                    }
                }
            }
        }

        @SuppressWarnings("PMD.EmptyCatchBlock")
        private void forceClose(Throwable error) throws InterruptedException {
            try {
                nioChannel.close();
                connState = ConnectionState.CLOSED;
            } catch (IOException e) {
                // Closed only to make sure, any failure can be ignored.
            }
            if (removeChannel(this)) {
                Closed evt = new Closed(error);
                downPipeline.fire(evt, this);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jgrapes.io.IOSubchannel.DefaultSubchannel#toString()
         */
        @SuppressWarnings("PMD.CommentRequired")
        public String toString() {
            return IOSubchannel.toString(this);
        }
    }

}
