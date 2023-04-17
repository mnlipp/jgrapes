package org.jgrapes.io.test.process;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.ProcessExited;
import org.jgrapes.io.events.ProcessStarted;
import org.jgrapes.io.events.StartProcess;
import org.jgrapes.io.process.ProcessManager;
import org.jgrapes.io.util.ByteBufferOutputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class BasicProcessTests {

    public static class Producer extends Component {

        public Producer(ProcessManager app) {
            super(app);
        }

        @Handler
        public void onProcessStarted(ProcessStarted event, IOSubchannel channel)
                throws IOException {
            try (var out
                = new OutputStreamWriter(new ByteBufferOutputStream(channel))) {
                out.write("Hello World!\n");
            }
        }
    }

    public static class Consumer extends Component {

        public StringBuilder collectedOut = new StringBuilder();
        public StringBuilder collectedErr = new StringBuilder();
        public int exitValue = -1;
        public boolean stdErrClosed;
        public boolean stdOutClosed;

        public Consumer(Channel app) {
            super(app);
        }

        @Handler
        public void onInput(Input<ByteBuffer> event)
                throws UnsupportedEncodingException {
            int length = event.data().limit();
            byte[] bytes = new byte[length];
            event.buffer().backingBuffer().get(bytes);
            switch (event.associated(FileDescriptor.class, Integer.class)
                .orElse(-1)) {
            case 1:
                collectedOut.append(new String(bytes));
                break;
            case 2:
                collectedErr.append(new String(bytes));
                break;
            }
        }

        @Handler
        public void onClosed(Closed<?> event) {
            switch (event.associated(FileDescriptor.class, Integer.class)
                .orElse(-1)) {
            case 1:
                stdOutClosed = true;
                break;
            case 2:
                stdErrClosed = true;
                break;
            }

        }

        @Handler
        public void onProcessExited(ProcessExited event)
                throws InterruptedException {
            exitValue = event.exitValue();
        }
    }

    @Test
    public void testCat() throws InterruptedException, IOException {
        var app = new ProcessManager();
        app.attach(new Producer(app));
        var consumer = new Consumer(app);
        app.attach(consumer);
        Components.start(app);
        app.fire(new StartProcess("cat", "-")).get();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(0, consumer.exitValue);
        assertEquals("Hello World!\n", consumer.collectedOut.toString());
        assertTrue(consumer.stdOutClosed);
        assertTrue(consumer.stdErrClosed);
    }

    @Test
    public void testCat2Err() throws InterruptedException, IOException {
        var app = new ProcessManager();
        app.attach(new Producer(app));
        var consumer = new Consumer(app);
        app.attach(consumer);
        Components.start(app);
        app.fire(new StartProcess("sh", "-c", "cat >&2")).get();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(0, consumer.exitValue);
        assertEquals("Hello World!\n", consumer.collectedErr.toString());
        assertTrue(consumer.stdOutClosed);
        assertTrue(consumer.stdErrClosed);
    }
}
