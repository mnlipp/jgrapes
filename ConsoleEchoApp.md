---
layout: default
title: JGrapes Console Echo Example
---

Console Echo
============

Console Echo is a slightly more complicated application that
uses two components. One component is the predefined
[InputStreamMonitor](latest-release/javadoc/index.html?org/jgrapes/io/InputStreamMonitor.html),
the other implements the application logic of this example.

```java
public class EchoUntilQuit extends Component {

    @Handler
    public void onInput(Input<ByteBuffer> event) {
        byte[] bytes = new byte[event.remaining()];
        event.data().get(bytes);
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
NIO package.

Once we have retrived the data, we write it to the console.
Then we check if the line entered was "`QUIT`". If this is the
case we fire a `Stop` event on the channel used by the application,
which causes it to terminate.

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
