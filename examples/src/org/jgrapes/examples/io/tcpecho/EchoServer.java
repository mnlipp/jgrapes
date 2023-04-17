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

package org.jgrapes.examples.io.tcpecho;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.net.SocketServer;

/**
 * An application that echoes data on a TCP connection.
 *
 * @startuml EchoServer.svg
 * 
 * object "app: EchoServer" as app
 * object "networkChannel: NamedChannel" as channel
 * object "connectionChannel: IOSubchannel" as subchannel
 * object "tcpServer: TcpServer" as tcpServer
 * object "dispatcher: NioDispatcher" as dispatcher
 * 
 * app *-- dispatcher
 * app *-- tcpServer
 * app -- channel: connected to >
 * tcpServer -up- channel: connected to >
 * subchannel -up-> channel
 * 
 * @enduml
 */
public class EchoServer extends Component {

    /**
     * @throws IOException 
     */
    public EchoServer(Channel componentChannel) throws IOException {
        super(componentChannel);
    }

    /**
     * Handle input data.
     *
     * @param event the event
     * @throws InterruptedException the interrupted exception
     */
    @Handler
    public void onRead(Input<ByteBuffer> event)
            throws InterruptedException {
        for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
            ManagedBuffer<ByteBuffer> out = channel.byteBufferPool().acquire();
            out.backingBuffer().put(event.buffer().backingBuffer());
            channel.respond(Output.fromSink(out, event.isEndOfRecord()));
        }
    }

    /**
     * @param args
     * @throws IOException 
     * @throws InterruptedException 
     */
    public static void main(String[] args)
            throws IOException, InterruptedException {
        Channel networkChannel = new NamedChannel("network i/o");
        Component app = new EchoServer(networkChannel)
            .attach(new NioDispatcher())
            .attach(new SocketServer(networkChannel).setServerAddress(
                new InetSocketAddress(8888)).setBufferSize(120000));
        Components.start(app);
        Components.awaitExhaustion();
    }

}
