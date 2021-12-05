package org.jgrapes.io.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ByteBufferOutputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class ByteBufferOutputTests {

    public static class Tracker extends Component {

        public int outputs = 0;
        public long collected = 0;
        public int eors = 0;
        public int closed = 0;

        public Tracker() {
            super(Channel.SELF);
        }

        @Handler
        public void onOutput(Output<ByteBuffer> event, IOSubchannel channel)
                throws UnsupportedEncodingException {
            outputs += 1;
            int length = event.data().limit();
            collected += length;
            if (event.isEndOfRecord()) {
                eors += 1;
            }
        }

        @Handler
        public void onClose(Close event, IOSubchannel channel) {
            closed += 1;
        }
    }

    @Test
    public void testDefaultFlushAndClose()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteBufferOutputStream out = new ByteBufferOutputStream(channel);
        out.write("Test".getBytes());
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // Flush again has no effect
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // eor sent, may not be repeated
        out.close();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals(1, tracker.eors);
        assertEquals(1, tracker.closed);
    }

    @Test
    public void testDefaultFlushAndCloseWithEmpty()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteBufferOutputStream out = new ByteBufferOutputStream(channel);
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected == 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // Flush again has no effect
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected == 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // eor sent, may not be repeated
        out.close();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals(1, tracker.eors);
        assertEquals(1, tracker.closed);
    }

    @Test
    public void testNoEorButClose()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteBufferOutputStream out = new ByteBufferOutputStream(channel);
        out.suppressEndOfRecord();
        out.write("Test".getBytes());
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(0, tracker.eors);
        assertEquals(0, tracker.closed);
        out.close();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals(0, tracker.eors);
        assertEquals(1, tracker.closed);
    }

    @Test
    public void testNoEorNoClose()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteBufferOutputStream out = new ByteBufferOutputStream(channel);
        out.suppressEndOfRecord().suppressClose();
        out.write("Test".getBytes());
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(0, tracker.eors);
        assertEquals(0, tracker.closed);
        out.close();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals(0, tracker.eors);
        assertEquals(0, tracker.closed);
    }

}
