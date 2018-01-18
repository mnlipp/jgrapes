---
layout: default
title: JGrapes Console Echo Example
---

Console Echo
============

Console Echo is a slightly more complicated application that
uses two components. One component is the predefined
[InputStreamMonitor](latest-release/javadoc/index.html?org/jgrapes/io/InputStreamMonitor.html),
the other makes up most of the example's code.

```java
public class EchoUntilQuit extends Component {

    @Handler
    public void onInput(Input<ByteBuffer> event) {
        byte[] bytes = new byte[event.remaining()];
        event.backingBuffer().get(bytes);
        String data = new String(bytes);
        System.out.print(data);
        if (data.trim().equals("QUIT")) {
            fire (new Stop());
        }
    }
}
```

Our component handles
[Input](latest-release/javadoc/index.html?org/jgrapes/io/events/Input.html)
events. Input events carry data that has been received from "outside"
the application. I/O data is represented using buffers from the
NIO package. While quite handy for efficient I/O, a drawback of the buffer
classes is that they are defined as classes and not using interfaces,
which makes things a bit more complicated.

JGrapes manages buffers in pools[^whyPools]. The framework therefore
defines the 
[ManagedBuffer](latest-release/javadoc/index.html?org/jgrapes/io/util/ManagedBuffer.html)
that wraps a NIO buffer, adding the information required for 
managing the buffer. The backing NIO buffer of the managed buffer can
be obtained with method `backingBuffer()`. For ease of use, as many methods
of `Buffer` as possible are made available as methods of `ManagedBuffer`
that simply delegate to `backingBuffer()`. However, methods of the
derived buffer types `ByteBuffer` and `CharBuffer` can only be accessed by 
calling `backingBuffer()` first.

An `Input` event has an associated `ManagedBuffer` that is accessible
with the method `buffer()`. Again, for ease of use, as many methods
as possible are made available as methods of `Input`. These
delegate to the managed buffer and from there to the backing
NIO buffer. That's why we can use `event.remaining()` to find out
the number of bytes available in the backing NIO buffer,
but we have to use `event.backingBuffer()` to actually
`get` them.

Once we have retrived the data, we write it to the console.
Then we check if the liine entered was "`QUIT`". If this is the
case we fire a `Stop` event on the channel used by the application,
which causes it to terminate.

[^whyPools]: Not because it wants to avoid garbage collection, but for 
    shaping streams of data. Imagine a pipeline where stage 1 produces
    data much faster than stage 2 can handle it. If we allowed
    arbitrary buffer allocation, it might happen that a lot of memory 
    is used for buffers created by stage 1 and not yet consumed by stage 2.

    Using a buffer pool limits the the production rate of stage 1 without
    reducing the overall performance. When all buffers are used, stage 1
    has to wait until some data is consumed by stage 2. But as soon as
    this is the case, it can continue to produce data in parallel (unless
    you set the pool size to 1, of course).

The two components are created and "connected" in a `main` method.

```java
    public static void main(String[] args) throws InterruptedException {
        EchoUntilQuit app = new EchoUntilQuit();
        app.attach(new InputStreamMonitor(app.channel(), System.in));
        Components.start(app);
        Components.awaitExhaustion();
        System.exit(0);
    }
```


![Structure](ConsoleEchoApp.svg)

*ToDo*

---
