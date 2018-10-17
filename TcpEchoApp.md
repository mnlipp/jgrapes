---
layout: default
title: JGrapes TCP Echo Server Example
---

TCP Echo Server
===============

The TCP Echo Server echoes the data received on a TCP connection. The 
application logic is implemented by the `EchoServer` component. The TCP
connections are managed by the 
[TcpServer](latest-release/javadoc/index.html?org/jgrapes/net/TcpServer.html)
component, which is part of the framework.

![Structure](EchoServer.svg)

The components are created and connected to a channel in the `main` 
method[^NioDispatcher].


```java
    public static void main(String[] args)
            throws IOException, InterruptedException {
        Channel networkChannel = new NamedChannel("network i/o");
        Component app = new EchoServer(networkChannel)
            .attach(new NioDispatcher())
            .attach(new TcpServer(networkChannel).setServerAddress(
                new InetSocketAddress(8888)).setBufferSize(120000));
        Components.start(app);
        Components.awaitExhaustion();
    }
```

The new challenge in this application is (compared to the "Console Echo"
example) that there can be multiple connections in parallel. If all input 
data from all clients was simply sent over the channel as in the previous
example, the data from different clients would be mixed up. Considering 
that data is provided by input events sent to a channel, the information 
about the client could be associated with the event or the channel. 
JGrapes takes the latter approach by introducing
[IOSubchannel](latest-release/javadoc/index.html?org/jgrapes/io/IOSubchannel.html)s.

`IOSubchannels` delegate the invocations of a channel's methods to their 
associated "main" channel. This causes events to be propagated according
to the associations between components and the "main" channel. However,
when an event is delivered to a handler, the channel that it is associated
with is the `IOSubChannel` that the event was fired on.

In our example application, the `TcpServer` creates a new `IOSubchannel` 
instance (with `networkChannel`
as "main" channel) for each incoming connection (a sample instance is shown 
as `connectionChannel` in the object diagram). The server then fires all
connection related events on this `IOSubchannel`. 

A component that handles events (such as the `EchoServer`) does not simply
assume that events have been fired on the channel that it is connected to
(and thus "listens on"). Instead it retrieves the channel that the event
was fired on from the event[^retrievedChannel].

```java
public class EchoServer extends Component {

    public EchoServer(Channel componentChannel) throws IOException {
        super(componentChannel);
    }

    @Handler
    public void onRead(Input<ByteBuffer> event)
            throws InterruptedException {
        for (IOSubchannel channel : event.channels(IOSubchannel.class)) {
            ManagedBuffer<ByteBuffer> out = channel.byteBufferPool().acquire();
            out.backingBuffer().put(event.buffer().backingBuffer());
            channel.respond(Output.fromSink(out, event.isEndOfRecord()));
        }
    }
}
```

Because events can be fired on several channels (consider a publish
subscribe system) a handler should loop over all channels associated
with the event, as shown for the `onRead` method above.

This pattern is required frequently, and the framework therefore supports
handler methods with a second parameter of type `IOSubchannel`.
Such handlers are invoked (with the same event instance) for 
every `IOSubchannel` that the event
was fired on. The `onRead` method can therefore be rewritten as:

```java
    @Handler
    public void onRead(Input<ByteBuffer> event, IOSubchannel channel)
            throws InterruptedException {
        ManagedBuffer<ByteBuffer> out = channel.byteBufferPool().acquire();
        out.backingBuffer().put(event.buffer().backingBuffer());
        channel.respond(Output.fromSink(out, event.isEndOfRecord()));
    }
```

Aside from grouping avents, an `IOSubchannel` provides some useful
features to the event handler. The first is a managed buffer pool
that the event handler can use to acquire buffers that it needs
for forwarding the (somehow processed) input data. In the example,
the handler simply copies the data into an acquired buffer and
uses it to create an
[Output](latest-release/javadoc/index.html?org/jgrapes/io/events/Output.html)
event. Using buffers from a buffer pool implies a flow control.
The size of the pool controls how many pending output events there
may be before the processing of input data stops.

As second major additional feature, an `IOSubchannel` provides a
"response pipeline". It hasn't been explicitly mentioned yet, but
events are processed, i.e. handlers are invoked by
[EventPipeline](latest-release/javadoc/index.html?org/jgrapes/core/EventPipeline.html)s,
whith each pipeline being driven by its own Java thread.
Using several pipelines introduces parallel processing of events.
Actually, the `TcpServer` creates a new pipeline for each incoming
connection. All connections are therefore automatically processed 
in parallel.

While the pipeline that delivers the input event to the handler could
also be used to process the generated output event, it increases
parallelism to use a different pipeline. This effectively means that
the processing of the next chunk of input data happens in parallel to
the delivery of the previous chunk of output data. All the programmer
has to do is to fire the output event on another pipeline. Such a pipeline
is provided for his convenience by `IOSubchannel.responsePipeline()`. As
an additional convenience, `subchannel.responsePipline().fire(event, subchannel)`
can be abbreviated as `subchannel.respond(event)`, as shown in `onRead` above.

---

[^NioDispatcher]: In addition, a 
    [NioDispatcher](latest-release/javadoc/index.html?org/jgrapes/io/NioDispatcher.html)
    is added to the component tree. One instance of this component type is 
    required in a component tree by the framework's I/O components such as
    the `Tcpserver`. How these components make use of the `NioDispatche` is beyond
    the scope of this introduction. 

[^retrievedChannel]: Obviously, the retrived channel is either the
    channel that the component listens on, or an
    `IOSubChannel` that has this channel as its "main" channel.
