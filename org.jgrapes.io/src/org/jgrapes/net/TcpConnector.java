/*
 * JGrapes Event driven Framework
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jgrapes.net;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.IOError;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.OpenTcpConnection;
import org.jgrapes.net.events.Connected;

/**
 * A component that reads from or write to a TCP connection.
 */
public class TcpConnector extends TcpConnectionManager {

    private int bufferSize = 1536;

    /**
     * Create a new instance using the given channel.
     * 
     * @param channel the component's channel 
     */
    public TcpConnector(Channel channel) {
        super(channel);
    }

    /**
     * Creates a new connector, using itself as component channel. 
     */
    public TcpConnector() {
        this(Channel.SELF);
    }

    /**
     * Sets the buffer size for the send an receive buffers.
     * If no size is set, the system defaults will be used.
     * 
     * @param size the size to use for the send and receive buffers
     * @return the TCP connector for easy chaining
     */
    public TcpConnector setBufferSize(int size) {
        this.bufferSize = size;
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
     * Opens a connection to the end point specified in the event.
     *
     * @param event the event
     */
    @Handler
    public void onOpenConnection(OpenTcpConnection event) {
        try {
            SocketChannel socketChannel = SocketChannel.open(event.address());
            channels.add(new TcpChannel(socketChannel));
        } catch (IOException e) {
            fire(new IOError(event, "Failed to open TCP connection.", e));
        }
    }

    /**
     * Called when the new socket channel has successfully been registered
     * with the nio dispatcher.
     *
     * @param event the event
     * @throws InterruptedException the interrupted exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(channels = Self.class)
    public void onRegistered(NioRegistration.Completed event)
            throws InterruptedException, IOException {
        NioHandler handler = event.event().handler();
        if (!(handler instanceof TcpChannel)) {
            return;
        }
        if (event.event().get() == null) {
            fire(new Error(event, "Registration failed, no NioDispatcher?",
                new Throwable()));
            return;
        }
        TcpChannel channel = (TcpChannel) handler;
        channel.registrationComplete(event.event());
        channel.downPipeline()
            .fire(new Connected(channel.nioChannel().getLocalAddress(),
                channel.nioChannel().getRemoteAddress()), channel);
    }

    /**
     * Shuts down the one of the connections.
     *
     * @param event the event
     * @throws IOException if an I/O exception occurred
     * @throws InterruptedException if the execution was interrupted
     */
    @Handler
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void onClose(Close event) throws IOException, InterruptedException {
        for (Channel channel : event.channels()) {
            if (channel instanceof TcpChannel && channels.contains(channel)) {
                ((TcpChannel) channel).close();
            }
        }
    }
}
