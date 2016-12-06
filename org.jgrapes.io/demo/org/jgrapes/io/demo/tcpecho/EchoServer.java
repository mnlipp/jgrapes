/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
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
package org.jgrapes.io.demo.tcpecho;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedByteBuffer;
import org.jgrapes.net.Server;

/**
 * @author Michael N. Lipp
 *
 */
public class EchoServer extends Component {

	/**
	 * @throws IOException 
	 */
	public EchoServer() throws IOException {
		super(Server.DEFAULT_CHANNEL);
		attach(new NioDispatcher());
		attach(new Server(new InetSocketAddress(8888), 120000));
	}

	@Handler
	public void onRead(Input<ManagedByteBuffer> event)
			throws InterruptedException {
		for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
			ManagedByteBuffer out = channel.bufferPool().acquire();
			out.put(event.getBuffer());
			channel.fire(new Output<>(out, event.isEndOfRecord()));
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
