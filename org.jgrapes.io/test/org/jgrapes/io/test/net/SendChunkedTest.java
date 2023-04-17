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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.NioDispatcher;
import org.jgrapes.io.test.WaitForTests;
import org.jgrapes.io.util.ByteBufferOutputStream;
import org.jgrapes.net.SocketServer;
import org.jgrapes.net.events.Accepted;
import org.jgrapes.net.events.Ready;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SendChunkedTest {

//	private static boolean localLogging = false;
//	
//	@BeforeClass
//	public static void enableLogging() throws FileNotFoundException {
//		Logger logger = Logger.getLogger("org.jgrapes");
//		if (logger.isLoggable(Level.FINE)) {
//			// Loggin already enabled
//			return;
//		}
//		localLogging = true;
//		System.setProperty("java.util.logging.SimpleFormatter.format",
//				"%1$tY-%1$tm-%1$td %5$s%n");
//		java.util.logging.Handler handler = new ConsoleHandler();
//		handler.setLevel(Level.FINEST);
//		handler.setFormatter(new SimpleFormatter());
//		logger.addHandler(handler);
//		logger.setUseParentHandlers(false);
//		logger.setLevel(Level.FINEST);
//	}
//
//	@AfterClass
//	public static void disableLogging() {
//		if (!localLogging) {
//			return;
//		}
//		System.setProperty("java.util.logging.SimpleFormatter.format",
//				"%1$tY-%1$tm-%1$td %5$s%n");
//		Logger logger = Logger.getLogger("org.jgrapes");
//		logger.setLevel(Level.INFO);
//		localLogging = false;		
//	}

    public class EchoServer extends Component {

        /**
         * @throws IOException 
         */
        public EchoServer(int bufferSize) throws IOException {
            super();
            SocketServer server = new SocketServer(this);
            if (bufferSize > 0) {
                server.setBufferSize(bufferSize);
            }
            attach(server);
        }

        @Handler
        public void onAcctepted(Accepted event)
                throws IOException, InterruptedException {
            for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
                try (ByteBufferOutputStream out = new ByteBufferOutputStream(
                    channel, newEventPipeline())) {
                    for (int i = 0; i < 1000000; i++) {
                        out.write(new String(i + ":Hello World!\n").getBytes());
                    }
                }
            }
        }
    }

    /**
     * Sends a lot of data. Use default buffer size to make sure it works
     * in general. Then use big buffer to make sure that the data cannot 
     * be sent to the network with a single write. Only then will the selector 
     * generate write the ops that we want to test here. 
     * 
     * @param event
     * @throws IOException
     * @throws InterruptedException
     */
    @ParameterizedTest
    @ValueSource(ints = { -1, 10 * 1024 * 1024 })
    @Timeout(5)
    public void sendTest(int bufferSize)
            throws IOException, InterruptedException, ExecutionException {
        EchoServer app = new EchoServer(bufferSize);
        app.attach(new NioDispatcher());
        WaitForTests<Ready> wf = new WaitForTests<>(
            app, Ready.class, app.defaultCriterion());
        Components.start(app);
        Ready readyEvent = (Ready) wf.get();
        if (!(readyEvent.listenAddress() instanceof InetSocketAddress)) {
            fail("");
        }
        InetSocketAddress serverAddr
            = ((InetSocketAddress) readyEvent.listenAddress());

        // There was a problem on the CI host only, difficult
        // to analyze. Therefore taking the buffered approach.
        StringBuilder received = new StringBuilder();
        try (Socket client = new Socket("localhost", serverAddr.getPort())) {
            InputStream fromServer = client.getInputStream();
            BufferedReader in = new BufferedReader(
                new InputStreamReader(fromServer, "ascii"));
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                received.append(line);
                received.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String data = received.toString();
        String previous = "(No previous line)";
        StringTokenizer lines = new StringTokenizer(data, "\n");
        int lineCount = 0;
        while (lineCount < 1_000_000 && lines.hasMoreElements()) {
            String line = lines.nextToken();
            String[] parts = line.split(":");
            try {
                int recvd = Integer.parseInt(parts[0]);
                assertEquals(lineCount, recvd);
            } catch (NumberFormatException e) {
                // To get a more informative message.
                fail("Invalid: \"" + line + "\" after \"" + previous + "\" ");
            }
            assertEquals("Hello World!", parts[1]);
            lineCount += 1;
            previous = line;
        }
        assertEquals(1_000_000, lineCount);

        // Test stop
        Components.manager(app).fire(new Stop(), Channel.BROADCAST);
        long waitEnd = System.currentTimeMillis() + 3000;
        while (true) {
            long waitTime = waitEnd - System.currentTimeMillis();
            if (waitTime <= 0) {
                fail();
            }
            try {
                assertTrue(Components.awaitExhaustion(waitTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }
    }

}
