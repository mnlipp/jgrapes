package org.jgrapes.io.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.Output;
import org.jgrapes.io.util.InputStreamPipeline;
import static org.junit.Assert.*;
import org.junit.Test;

public class InputStreamTests {

    public static class Tracker extends Component {

        public int outputs = 0;
        public int inputs = 0;
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
        public void onInput(Input<ByteBuffer> event, IOSubchannel channel)
                throws UnsupportedEncodingException {
            inputs += 1;
            int length = event.data().limit();
            collected += length;
            if (event.isEndOfRecord()) {
                eors += 1;
            }
        }

        @Handler
        public void onClosed(Closed event, IOSubchannel channel) {
            closed += 1;
        }
    }

    @Test
    public void testEorAndClose()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteArrayInputStream in = new ByteArrayInputStream("Test".getBytes());
        InputStreamPipeline isp = new InputStreamPipeline(in, channel);
        isp.run();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(1, tracker.eors);
        assertEquals(1, tracker.closed);
    }

    @Test
    public void testEorNoClose()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteArrayInputStream in = new ByteArrayInputStream("Test".getBytes());
        InputStreamPipeline isp = new InputStreamPipeline(in, channel)
            .suppressClosed();
        isp.run();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
    }

    @Test(timeout = 1000)
    public void testSeveralBuffers()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        byte[] data
            = new byte[(int) (channel.byteBufferPool().bufferSize() * 2.5)];
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        InputStreamPipeline isp = new InputStreamPipeline(in, channel)
            .suppressClosed();
        isp.run();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(3, tracker.outputs);
        assertTrue(tracker.collected > 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // Two must be available
        channel.byteBufferPool().acquire();
        channel.byteBufferPool().acquire();
    }

    @Test(timeout = 1000)
    public void testEmptyInput()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        byte[] data = new byte[0];
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        InputStreamPipeline isp = new InputStreamPipeline(in, channel)
            .suppressClosed();
        isp.run();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.outputs);
        assertTrue(tracker.collected == 0);
        assertEquals(1, tracker.eors);
        assertEquals(0, tracker.closed);
        // Two must be available
        channel.byteBufferPool().acquire();
        channel.byteBufferPool().acquire();
    }

    @Test
    public void testInputEorAndClose()
            throws InterruptedException, IOException {
        Tracker tracker = new Tracker();
        Components.start(tracker);
        IOSubchannel channel = IOSubchannel.create(
            tracker, tracker.newEventPipeline());
        ByteArrayInputStream in = new ByteArrayInputStream("Test".getBytes());
        InputStreamPipeline isp = new InputStreamPipeline(in, channel);
        isp.sendInputEvents();
        isp.run();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(1, tracker.inputs);
        assertTrue(tracker.collected > 0);
        assertEquals(1, tracker.eors);
        assertEquals(1, tracker.closed);
    }

}
