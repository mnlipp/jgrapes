package org.jgrapes.io.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import org.jgrapes.io.util.LineCollector;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import static org.junit.Assert.*;
import org.junit.Test;

public class LineCollectorTests {

    @Test
    public void testWriteAndClose() throws InterruptedException, IOException {
        LineCollector collector = new LineCollector();
        var charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
            () -> CharBuffer.allocate(4096), 2).setName("Test");

        // First feed
        var data = charBufferPool.acquire();
        data.backingBuffer().append("Hello");
        data.backingBuffer().flip();
        collector.feed(data);
        data.unlockBuffer();

        // Second feed
        data = charBufferPool.acquire();
        data.backingBuffer().append(" World!\n");
        data.backingBuffer().flip();
        collector.feed(data);
        data.unlockBuffer();

        // End of feed
        collector.feed((CharBuffer) null);
        assertTrue(collector.eof());
        assertEquals("Hello World!", collector.getLine());
    }

    @Test
    public void testByteBuffer() throws InterruptedException, IOException {
        LineCollector collector = new LineCollector();
        var byteBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
            () -> ByteBuffer.allocate(4096), 2).setName("Test");

        // First feed
        var data = byteBufferPool.acquire();
        String in = "Special chars: äöü";
        data.backingBuffer().put(in.getBytes(StandardCharsets.UTF_8));
        data.backingBuffer().flip();
        collector.feed(data);
        data.unlockBuffer();

        // Second feed
        data = byteBufferPool.acquire();
        in = "ÄÖÜß.\n";
        data.backingBuffer().put(in.getBytes(StandardCharsets.UTF_8));
        data.backingBuffer().flip();
        collector.feed(data);
        data.unlockBuffer();

        // End of feed
        collector.feed((ByteBuffer) null);
        assertTrue(collector.eof());
        assertEquals("Special chars: äöüÄÖÜß.", collector.getLine());
    }

}
