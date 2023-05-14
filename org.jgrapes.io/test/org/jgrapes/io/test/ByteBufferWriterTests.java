package org.jgrapes.io.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.ByteBufferWriter;
import static org.junit.Assert.*;
import org.junit.Test;

public class ByteBufferWriterTests {

    private static Charset CHARSET = Charset.forName("iso-8859-15");

    public static class Tracker extends Component {

        public int outputs = 0;
        public String collected = "";
        public int eors = 0;
        public int closed = 0;

        public Tracker() {
            super(Channel.SELF);
        }

        @Handler
        public void onOutput(Output<ByteBuffer> event, IOSubchannel channel)
                throws UnsupportedEncodingException {
            outputs += 1;
            var backing = event.buffer().backingBuffer();
            collected += new String(backing.array(), backing.position(),
                backing.limit(), CHARSET);
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
    public void testManyEvents() throws InterruptedException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        try (@SuppressWarnings("resource")
        var out = new ByteBufferWriter(channel).charset(CHARSET)) {
            for (int i = 0; i < 10000; i++) {
                out.write("Special: äöüÄÖÜß€");
            }
        }
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertTrue(tracker.outputs > 1);
        assertEquals(10000 * "Special: äöüÄÖÜß€".length(),
            tracker.collected.length());
        assertEquals(1, tracker.eors);
        assertEquals(1, tracker.closed);
    }

    @Test
    public void testDefaultFlushAndClose()
            throws InterruptedException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        @SuppressWarnings("resource")
        var out = new ByteBufferWriter(channel).charset(CHARSET);
        out.write("Special: äöüÄÖÜß€");
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals("Special: äöüÄÖÜß€", tracker.collected);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // Flush again has no effect
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals("Special: äöüÄÖÜß€", tracker.collected);
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
        @SuppressWarnings("resource")
        var out = new ByteBufferWriter(channel).charset(CHARSET);
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals("", tracker.collected);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // Flush again has no effect
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals("", tracker.collected);
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
        @SuppressWarnings("resource")
        var out = new ByteBufferWriter(channel).charset(CHARSET);
        out.suppressEndOfRecord();
        out.write("Special: äöüÄÖÜß€");
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals("Special: äöüÄÖÜß€", tracker.collected);
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
        @SuppressWarnings("resource")
        var out = new ByteBufferWriter(channel).charset(CHARSET);
        out.suppressEndOfRecord().suppressClose();
        out.write("Special: äöüÄÖÜß€");
        out.flush();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertEquals("Special: äöüÄÖÜß€", tracker.collected);
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
