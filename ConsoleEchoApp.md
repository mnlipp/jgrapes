---
layout: default
title: JGrapes Console Echo Example
---

Console Echo
============

Console Echo is a slightly more complicated application that
uses two components. One component is the 
[InputStreamMonitor](latest-release/javadoc/index.html?org/jgrapes/io/InputStreamMonitor.html) from the framework,
the other is `EchoUntilQuit`, which implements the application logic of 
this example.

![Structure](ConsoleEchoApp.svg)

The two components are created and connected to a channel in the `main` method.

```java
    public static void main(String[] args) throws InterruptedException {
        Channel channel = new NamedChannel("main");
        EchoUntilQuit app = new EchoUntilQuit(channel);
        app.attach(new InputStreamMonitor(channel, System.in));
        Components.start(app);
        Components.awaitExhaustion();
        System.exit(0);
    }
```

Attaching the `InputStreamMonitor` to the `EchoUntilQuit` component 
establishes the composition relationship. The components' connection
to their associated channel is established by passing the channel
as argument to the component's constructor.

The "application logic" is provided by the handler method of `EchoUntilQuit`.

```java
public class EchoUntilQuit extends Component {

    public EchoUntilQuit(Channel channel) {
        super(channel);
    }

    @Handler
    public void onInput(Input<ByteBuffer> event) {
        String data = Charset.defaultCharset().decode(event.data()).toString();
        System.out.print(data);
        if (data.trim().equals("QUIT")) {
            fire (new Stop());
        }
    }
}
```

[Input](latest-release/javadoc/index.html?org/jgrapes/io/events/Input.html)
events are created by the `InputStreamMonitor` 
in response to text entered in the console and are fired on the channel
that the `InputStreamMonitor` is connected to. `EchoUnitQuit`'s handler is 
invoked for all these events. The handler retrieves the data, which is 
contained in the event as a buffer from the Java 
[NIO package](https://docs.oracle.com/javase/8/docs/api/index.html?java/nio/package-summary.html) and writes it to the console. Then the handler checks if the 
line entered was "`QUIT`". If this is the case, it fires a `Stop` event 
on the application's channel.

The `Stop` event is processed by a handler method of `InputStreamMonitor` and 
causes the component to stop listening for data from the console. This
puts the application in a state where all events are processed and 
there are no more components that can generate events. This is the
state that the invocation of `Components.awaitExhaustion()` 
(in the `main` method above) waits for. As this state is reached now, the
`main` method continues and the application terminates.

---
