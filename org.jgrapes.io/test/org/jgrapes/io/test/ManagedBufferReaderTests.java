package org.jgrapes.io.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.time.Duration;
import org.jgrapes.core.Components;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.io.util.ManagedBufferReader;
import static org.junit.Assert.*;
import org.junit.Test;

public class ManagedBufferReaderTests {

    public static class ReaderThread extends Thread {
        private Reader reader;
        public StringBuilder received = new StringBuilder();
        public boolean gotEof;

        public ReaderThread(Reader reader) {
            super();
            this.reader = reader;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(reader)) {
                while (true) {
                    String line = in.readLine();
                    if (line == null) {
                        gotEof = true;
                        break;
                    }
                    received.append(line).append('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testWriteAndClose() throws InterruptedException, IOException {
        ManagedBufferReader reader = new ManagedBufferReader();
        ReaderThread readerThread = new ReaderThread(reader);
        readerThread.start();
        var charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
            () -> CharBuffer.allocate(4096), 2).setName("Test");

        // First feed
        var data = charBufferPool.acquire();
        data.backingBuffer().append("Hello");
        data.backingBuffer().flip();
        reader.feed(data);
        data.unlockBuffer();

        // Second feed
        data = charBufferPool.acquire();
        data.backingBuffer().append(" World!\n");
        data.backingBuffer().flip();
        reader.feed(data);
        data.unlockBuffer();

        // End of feed
        reader.feed(null);
        readerThread.join(1000);
        assertTrue(readerThread.gotEof);
        assertEquals("Hello World!\n", readerThread.received.toString());

        // Indirect check: all buffers unlocked, i.e. available
        var thread = Thread.currentThread();
        var timer = Components.schedule(t -> thread.interrupt(),
            Duration.ofMillis(100));
        try {
            charBufferPool.acquire();
            charBufferPool.acquire();
        } catch (InterruptedException e) {
            fail();
        } finally {
            timer.cancel();
        }
    }

}
