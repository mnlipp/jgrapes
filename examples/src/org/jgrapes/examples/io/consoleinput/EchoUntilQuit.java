/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.examples.io.consoleinput;

import java.nio.ByteBuffer;

import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.InputStreamMonitor;
import org.jgrapes.io.events.Input;

/**
 * An application that echos data from the console until a line
 * with "QUIT" is received.
 * 
 * @startuml ConsoleEchoApp.svg
 * 
 * object "app: EchoUntilQuit" as app
 * object "console: InputStreamMonitor" as console
 * object "channel: Channel" as channel
 * 
 * app -- console
 * app .. channel
 * console .up. channel
 * 
 * note "Besides the (structural) parent-child \n\
 *   association the two components are \n\
 *   logically connected by the channel" as ChannelNote
 * 
 * channel .. ChannelNote
 * 
 * @enduml
 */
public class EchoUntilQuit extends Component {

	@Handler
	public void onInput(Input<ByteBuffer> event) {
		byte[] bytes = new byte[event.remaining()];
		event.backingBuffer().get(bytes);
		String data = new String(bytes);
		System.out.print(data);
		if (data.trim().equals("QUIT")) {
			fire (new Stop());
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws InterruptedException the interrupted exception
	 */
	public static void main(String[] args) throws InterruptedException {
		EchoUntilQuit app = new EchoUntilQuit();
		app.attach(new InputStreamMonitor(app.channel(), System.in));
		Components.start(app);
		Components.awaitExhaustion();
		System.exit(0);
	}
}
