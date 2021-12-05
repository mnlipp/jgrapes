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

package org.jgrapes.io.test.net;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.net.TcpServer;
import org.jgrapes.util.JsonConfigurationStore;
import static org.junit.Assert.*;
import org.junit.Test;

public class ConfigureTcpServerTest {

    @Test
    public void testConfigure() throws InterruptedException, IOException {
        TcpServer app = new TcpServer(Channel.SELF).setServerAddress(
            new InetSocketAddress("test.org", 80));
        app.attach(new NioDispatcher());
        File file = new File("test-resources/TestConfig.json");
        app.attach(new JsonConfigurationStore(app, file, false));
        Components.start(app);
        assertEquals("127.0.0.1", app.serverAddress().getHostString());
        assertEquals(123, app.backlog());
        assertEquals(4567, app.bufferSize());
        Components.manager(app).fire(new Stop(), Channel.BROADCAST);
        Components.awaitExhaustion(1000);
        Components.checkAssertions();
    }

}
