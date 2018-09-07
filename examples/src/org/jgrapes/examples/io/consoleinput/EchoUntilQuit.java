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
import java.nio.charset.Charset;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.NamedChannel;
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
 * app *-- console
 * app -- channel: connected to >
 * console -up- channel: connected to >
 * 
 * note top of channel:	Serves as\ncommunication bus
 * 
 * @enduml
 */
public class EchoUntilQuit extends Component {

    public EchoUntilQuit(Channel channel) {
        super(channel);
    }

    @Handler
    public void onInput(Input<ByteBuffer> event) {
        String data = Charset.defaultCharset().decode(event.data()).toString();
        System.out.print(data);
        if (data.trim().equals("QUIT")) {
            fire(new Stop());
        }
    }

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws InterruptedException the interrupted exception
     */
    public static void main(String[] args) throws InterruptedException {
        Channel channel = new NamedChannel("main");
        EchoUntilQuit app = new EchoUntilQuit(channel);
        app.attach(new InputStreamMonitor(channel, System.in));
        Components.start(app);
        Components.awaitExhaustion();
    }
}
