package org.jgrapes.io.test;

import java.io.IOException;
import java.nio.CharBuffer;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.io.util.ManagedBufferStreamer;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ManagedBufferStreamerTests {

    private String result;

    @Test(timeout = 1000)
    public void test() throws InterruptedException, IOException {
        var charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
            () -> CharBuffer.allocate(4096), 2).setName("Test");

        ManagedBufferStreamer streamer = new ManagedBufferStreamer(rdr -> {
            StringBuffer sb = new StringBuffer();
            try {
                while (true) {
                    int ch = rdr.read();
                    if (ch == -1) {
                        break;
                    }
                    sb.append((char) ch);
                }
            } catch (IOException e) {
                // ignore
            }
            result = sb.toString();
        });

        // Feed
        var data = charBufferPool.acquire();
        data.backingBuffer().append("Hello World!");
        data.backingBuffer().flip();
        streamer.feed(data);
        data.unlockBuffer();

        // End of feed
        streamer.feed((ManagedBuffer<?>) null);

        while (result == null) {
            Thread.sleep(10);
        }
        assertEquals("Hello World!", result);
    }

}
