package org.jgrapes.io.test;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Started;
import org.jgrapes.io.util.JsonReader;
import org.jgrapes.io.util.ManagedBuffer;
import org.jgrapes.io.util.ManagedBufferPool;
import org.jgrapes.io.util.events.DataInput;
import static org.junit.Assert.*;
import org.junit.Test;

public class JsonReaderTests {

    public class TestApp extends Component {

        @Handler
        public void onStarted(Started event, Channel channel)
                throws InterruptedException, IOException {
            registerAsGenerator();
            var charBufferPool = new ManagedBufferPool<>(ManagedBuffer::new,
                () -> CharBuffer.allocate(4096), 2).setName("Test");

            // Create reader
            JsonReader rdr
                = new JsonReader(Object.class, activeEventPipeline(), this);

            // Feed
            var data = charBufferPool.acquire();
            data.backingBuffer().append("{\"hello\": \"world\"}");
            data.backingBuffer().flip();
            rdr.feed(data);
            data.unlockBuffer();

            // End of feed
            rdr.feed(null);
        }

        @Handler
        public void onJson(DataInput<Object> event, Channel channel) {
            result = event.data();
            unregisterAsGenerator();
        }
    }

    private Object result;

    @SuppressWarnings("unchecked")
    @Test(timeout = 1000)
    public void test() throws InterruptedException, IOException {

        var app = new TestApp();
        Components.start(app);

        while (result == null) {
            Thread.sleep(10);
        }
        assertTrue(result instanceof Map);
        assertEquals("world", ((Map<String, String>) result).get("hello"));

        Components.awaitExhaustion();
    }

}
