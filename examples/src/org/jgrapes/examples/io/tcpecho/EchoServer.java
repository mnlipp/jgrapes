/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.examples.io.tcpecho;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.net.TcpServer;

/**
 *
 */
public class EchoServer extends Component {

	/**
	 * @throws IOException 
	 */
	public EchoServer() throws IOException {
		super();
		attach(new NioDispatcher());
		attach(new TcpServer(this).setServerAddress(
				new InetSocketAddress(8888)).setBufferSize(120000));
	}

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
	 */
	public static void main(String[] args) {
		try {
			EchoServer app = new EchoServer();
			Components.start(app);
			Components.awaitExhaustion();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

}
