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
import java.net.ConnectException;
import java.nio.channels.SocketChannel;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Self;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Error;
import org.jgrapes.io.NioHandler;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.ConnectError;
import org.jgrapes.io.events.NioRegistration;
import org.jgrapes.io.events.OpenTcpConnection;
import org.jgrapes.io.events.Opening;
import org.jgrapes.net.events.ClientConnected;
import org.jgrapes.net.events.Connected;

/**
 * A component that reads from or write to a TCP connection.
 */
public class TcpConnector extends TcpConnectionManager {

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

    @Override
    public TcpConnector setBufferSize(int size) {
        super.setBufferSize(size);
        return this;
    }

    /**
     * Opens a connection to the end point specified in the event.
     *
     * @param event the event
     */
    @Handler
    public void onOpenConnection(OpenTcpConnection event) {
        try {
            @SuppressWarnings("PMD.CloseResource")
            SocketChannel socketChannel = SocketChannel.open(event.address());
            channels.add(new TcpChannelImpl(event, socketChannel));
        } catch (ConnectException e) {
            fire(new ConnectError(event, "Connection refused.", e));
        } catch (IOException e) {
            fire(new ConnectError(event, "Failed to open TCP connection.", e));
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
        if (!(handler instanceof TcpChannelImpl)) {
            return;
        }
        if (event.event().get() == null) {
            fire(new Error(event, "Registration failed, no NioDispatcher?",
                new Throwable()));
            return;
        }
        TcpChannelImpl channel = (TcpChannelImpl) handler;
        Connected<?> connected;
        if (channel.openEvent().isPresent()) {
            connected = new ClientConnected(channel.openEvent().get(),
                channel.nioChannel().getLocalAddress(),
                channel.nioChannel().getRemoteAddress());
        } else {
            connected
                = new Connected<>(channel.nioChannel().getLocalAddress(),
                    channel.nioChannel().getRemoteAddress());
        }
        var registration = event.event().get();
        // (1) Opening, (2) Connected, (3) start processing input
        channel.downPipeline()
            .fire(Event.onCompletion(new Opening<Void>(), e -> {
                channel.downPipeline().fire(connected, channel);
                channel.registrationComplete(registration);
            }), channel);
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
            if (channel instanceof TcpChannelImpl
                && channels.contains(channel)) {
                ((TcpChannelImpl) channel).close();
            }
        }
    }
}
