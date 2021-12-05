/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2018 Michael N. Lipp
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

package org.jgrapes.http.freemarker.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.jgrapes.core.Component;
import org.jgrapes.http.HttpServer;
import org.jgrapes.http.events.Request;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.net.TcpServer;
import org.jgrapes.net.events.Ready;
import static org.junit.Assert.fail;

/**
 *
 */
public class BasicTestServer extends Component {
    private InetSocketAddress addr;
    private WaitForTests readyMonitor;

    @SafeVarargs
    public BasicTestServer(Class<? extends Request.In>... fallbacks)
            throws IOException, InterruptedException, ExecutionException {
        attach(new NioDispatcher());
        TcpServer networkServer = attach(new TcpServer());
        attach(new HttpServer(channel(), networkServer.channel(),
            fallbacks));
        readyMonitor = new WaitForTests(
            this, Ready.class, networkServer.channel().defaultCriterion());
    }

    private InetSocketAddress getSocketAddress()
            throws InterruptedException, ExecutionException {
        if (addr == null) {
            Ready readyEvent = (Ready) readyMonitor.get();
            if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
                fail();
            }
            addr = ((InetSocketAddress) readyEvent.listenAddress());
        }
        return addr;

    }

    public InetAddress getAddress()
            throws InterruptedException, ExecutionException {
        return getSocketAddress().getAddress();
    }

    public int getPort()
            throws InterruptedException, ExecutionException {
        return getSocketAddress().getPort();
    }
}
