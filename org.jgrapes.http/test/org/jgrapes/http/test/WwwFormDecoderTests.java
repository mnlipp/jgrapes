package org.jgrapes.http.test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import org.jgrapes.http.WwwFormUrldecoder;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import static org.junit.Assert.*;
import org.junit.Test;

public class WwwFormDecoderTests {

    @Test
    public void testWriteAndClose() throws InterruptedException, IOException {
        var charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
            () -> CharBuffer.allocate(4096), 2).setName("Test");

        // First feed
        var data = charBufferPool.acquire();
        data.backingBuffer().append("Test=usual");
        data.backingBuffer().append(
            "&Special=" + URLEncoder.encode("äöü", StandardCharsets.UTF_8));
        data.backingBuffer().append("&Hello");
        data.backingBuffer().flip();
        WwwFormUrldecoder decoder = new WwwFormUrldecoder();
        decoder.feed(data);
        data.unlockBuffer();

        // Second feed
        data = charBufferPool.acquire();
        data.backingBuffer().append("=World!\n");
        data.backingBuffer().flip();
        decoder.feed(data);
        data.unlockBuffer();

        // End of feed
        decoder.feed((ManagedBuffer<?>) null);
        assertTrue(decoder.eof());
        assertEquals("usual", decoder.result().get("Test").get(0));
        assertEquals("äöü", decoder.result().get("Special").get(0));
        assertEquals("World!", decoder.result().get("Hello").get(0));
    }

}
