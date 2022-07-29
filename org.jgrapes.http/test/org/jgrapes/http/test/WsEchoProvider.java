/*
 * Ad Hoc Polling Application
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

package org.jgrapes.http.test;

import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.jdrupes.httpcodec.protocols.http.HttpField;
import org.jdrupes.httpcodec.protocols.http.HttpRequest;
import org.jdrupes.httpcodec.types.Converters;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.http.Session;
import org.jgrapes.http.annotation.RequestHandler;
import org.jgrapes.http.events.ProtocolSwitchAccepted;
import org.jgrapes.http.events.Request;
import org.jgrapes.http.events.Upgraded;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ManagedBuffer;

/**
 * 
 */
public class WsEchoProvider extends Component {

    private Set<IOSubchannel> openChannels
        = Collections.newSetFromMap(new WeakHashMap<IOSubchannel, Boolean>());

    /**
     * @param componentChannel
     */
    public WsEchoProvider(Channel componentChannel) {
        super(componentChannel);
    }

    @RequestHandler(patterns = "/ws/echo", priority = 100)
    public void onGet(Request.In.Get event, IOSubchannel channel)
            throws InterruptedException {
        final HttpRequest request = event.httpRequest();
        if (request.queryData().containsKey("store")) {
            event.associated(Session.class).ifPresent(session -> session
                .put("stored", request.queryData().get("store").get(0)));
        }
        if (!request.findField(
            HttpField.UPGRADE, Converters.STRING_LIST)
            .map(f -> f.value().containsIgnoreCase("websocket"))
            .orElse(false)) {
            return;
        }
        openChannels.add(channel);
        channel.respond(new ProtocolSwitchAccepted(event, "websocket"));
        event.stop();
    }

    @Handler
    public void onUpgraded(Upgraded event, IOSubchannel channel) {
        if (!openChannels.contains(channel)) {
            return;
        }
        channel.respond(Output.from("/Greetings!", true));
    }

    @Handler
    public void onInput(Input<CharBuffer> event, IOSubchannel channel) {
        if (!openChannels.contains(channel)) {
            return;
        }
        ManagedBuffer<CharBuffer> out = ManagedBuffer.wrap(
            CharBuffer.wrap(event.data()));
        out.position(out.limit());
        out.flip();
        String line = out.backingBuffer().toString();
        if (line.compareToIgnoreCase("/stored") == 0) {
            channel.associated(Session.class, Supplier.class).ifPresent(
                supplier -> {
                    String stored
                        = (String) ((Session) supplier.get()).get("stored");
                    channel.respond(Output.from(stored, true));
                });
            return;
        }
        if (line.compareToIgnoreCase("/quit") == 0) {
            channel.respond(new Close());
            return;
        }
        channel.respond(Output.fromSource(out, true));
    }

    @Handler
    public void onClosed(Closed event, IOSubchannel channel) {
        openChannels.remove(channel);
    }
}
