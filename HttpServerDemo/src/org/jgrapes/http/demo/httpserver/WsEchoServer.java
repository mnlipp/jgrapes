/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017  Michael N. Lipp
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

package org.jgrapes.http.demo.httpserver;

import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.types.Converters;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.GetRequest;
import org.jgrapes.http.events.Upgraded;
import org.jgrapes.http.events.WebSocketAccepted;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedCharBuffer;

/**
 * 
 */
public class WsEchoServer extends Component {

	private Set<IOSubchannel> openChannels 
		= Collections.newSetFromMap(new WeakHashMap<IOSubchannel, Boolean>());
	
	/**
	 * 
	 */
	public WsEchoServer() {
	}

	/**
	 * @param componentChannel
	 */
	public WsEchoServer(Channel componentChannel) {
		super(componentChannel);
	}

	@RequestHandler(patterns="/ws/echo")
	public void onGet(GetRequest event, IOSubchannel channel) 
			throws InterruptedException {
		final HttpRequest request = event.request();
		if (!request.findField(
				HttpField.UPGRADE, Converters.STRING_LIST)
				.map(f -> f.value().containsIgnoreCase("websocket"))
				.orElse(false)) {
			return;
		}
		openChannels.add(channel);
		channel.respond(new WebSocketAccepted(event));
		event.stop();
	}
	
	@Handler
	public void onUpgraded(Upgraded event, IOSubchannel channel) {
		if (!openChannels.contains(channel)) {
			return;
		}
		ManagedCharBuffer greetings = new ManagedCharBuffer("/Greetings!");
		greetings.position(greetings.limit());
		channel.respond(new Output<>(greetings, true));
	}
	
	@Handler
	public void onInput(Input<ManagedCharBuffer> event, IOSubchannel channel) {
		if (!openChannels.contains(channel)) {
			return;
		}
		ManagedCharBuffer out = new ManagedCharBuffer(
				CharBuffer.wrap(event.buffer().backingBuffer()));
		out.position(out.limit());
		channel.respond(new Output<>(out, true));
	}
	
	@Handler
	public void onClosed(Closed event, IOSubchannel channel) {
		openChannels.remove(channel);
	}
}
