---
layout: default
title: JGrapes TCP Echo Server Example
---

TCP Echo Server
===============

The TCP Echo Server echoes the data received on a TCP connection. The 
application logic is implemented by the `EchoServer` component. The TCP
connections are managed by the 
[TcpServer](latest-release/javadoc/index.html?org/jgrapes/net/TcpServer.html).

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
example, the data would be mixed up. Considering that data is provided 
by events sent to a channel, the information about the client could be 
associated with the event or the channel. JGrapes takes the latter
approach by introducing
[IOSubchannel](latest-release/javadoc/index.html?org/jgrapes/io/IOSubchannel.html)s.

`Subchannels` delegate the invocations of a channel's methods to their 
associated "main" channel. This causes events to be propagated according
to the associations between components and the "main" channel. However,
when an event is delivered to a handler, the channel that it is associated
with is the `IOSubChannel` that the event was fired on.

In our example application, the `TcpServer` creates a new `IOSubchannel` 
instance (with `networkChannel`
as "main" channel) for each incoming connection (a sample instance is shown 
as `connectionChannel` in the object diagram). The server then fires all
connection related events on this `IOSubchannel`. 

A component that handles events (such as `EchoServer`) does not simply
assume that events have been fired on the channel that it "listens on"
(is associated with). Instead it retrieves the channel that the event
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

Because this pattern is required frequently, the framework supports
handler methods that have a second parameter of type `IOSubchannel`.
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

*To be completed*

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
