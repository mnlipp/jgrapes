package org.jgrapes.io.test.process;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Stop;
import org.jgrapes.io.events.Close;
import org.jgrapes.io.events.Closed;
import org.jgrapes.io.events.Input;
import org.jgrapes.io.events.ProcessExited;
import org.jgrapes.io.events.StartProcess;
import org.jgrapes.io.process.ProcessManager;
import static org.junit.Assert.*;
import org.junit.Test;

public class TerminateProcessTests {

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
            event.buffer().backingBuffer().mark();
            event.buffer().backingBuffer().get(bytes);
            event.buffer().backingBuffer().reset();
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
        public void onProcessExited(ProcessExited event)
                throws InterruptedException {
            exitValue = event.exitValue();
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
    }

    public static class OnStartedCloser extends Component {

        public OnStartedCloser(Channel componentChannel) {
            super(componentChannel);
        }

        @Handler
        public void onInput(Input<ByteBuffer> event, Channel channel)
                throws UnsupportedEncodingException {
            if (event.associated(FileDescriptor.class, Integer.class)
                .orElse(-1) != 1) {
                return;
            }
            int length = event.data().limit();
            byte[] bytes = new byte[length];
            event.buffer().backingBuffer().mark();
            event.buffer().backingBuffer().get(bytes);
            event.buffer().backingBuffer().reset();
            if (new String(bytes).startsWith("Started")) {
                fire(new Close().setAssociated(Process.class, true), channel);
            }

        }
    }

    @Test
    public void testClose() throws InterruptedException, IOException {
        var app = new ProcessManager();
        var consumer = new Consumer(app);
        app.attach(consumer);
        app.attach(new OnStartedCloser(app));
        Components.start(app);
        app.fire(new StartProcess("/bin/sh", "test-resources/destroy.sh"))
            .get();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(0, consumer.exitValue);
        assertEquals("TERMinated\n", consumer.collectedErr.toString());
        assertTrue(consumer.stdOutClosed);
        assertTrue(consumer.stdErrClosed);
    }

    public static class OnStartedStopper extends Component {

        public OnStartedStopper(Channel componentChannel) {
            super(componentChannel);
        }

        @Handler
        public void onInput(Input<ByteBuffer> event, Channel channel)
                throws UnsupportedEncodingException {
            if (event.associated(FileDescriptor.class, Integer.class)
                .orElse(-1) != 1) {
                return;
            }
            int length = event.data().limit();
            byte[] bytes = new byte[length];
            event.buffer().backingBuffer().mark();
            event.buffer().backingBuffer().get(bytes);
            event.buffer().backingBuffer().reset();
            if (new String(bytes).startsWith("Started")) {
                fire(new Stop(), channel);
            }

        }
    }

    @Test
    public void testStop() throws InterruptedException, IOException {
        var app = new ProcessManager();
        var consumer = new Consumer(app);
        app.attach(consumer);
        app.attach(new OnStartedStopper(app));
        Components.start(app);
        app.fire(new StartProcess("/bin/sh", "test-resources/destroy.sh"))
            .get();
        Components.awaitExhaustion();
        Components.checkAssertions();
        assertEquals(0, consumer.exitValue);
        assertEquals("TERMinated\n", consumer.collectedErr.toString());
        assertTrue(consumer.stdOutClosed);
        assertTrue(consumer.stdErrClosed);
    }
}
