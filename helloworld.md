---
layout: default
title: JGrapes Example
---

Hello World!
============

Here's our first component. 

```java
import org.jgrapes.core.Component;
import org.jgrapes.core.Components;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.core.events.Start;

public class Greeter extends Component {

    @Handler
    public void onStart(Start event) {
        System.out.println ("Hello World!");
    }
    
    public static void main(String[] args) 
            throws InterruptedException {
        Components.start(new Greeter());
    }
}
```

The easiest way to define a new component is to inherit from
[Component](latest-release/javadoc/index.html?org/jgrapes/core/Component.html).
Components define methods that can be annotated as
[handlers](latest-release/javadoc/index.html?org/jgrapes/core/annotation/Handler.html)
for
[events](latest-release/javadoc/index.html?org/jgrapes/core/Event.html).

A predefined event from the core package is the
[Start](latest-release/javadoc/index.html?org/jgrapes/core/events/Start.html)
event. This events triggers the event processing by the framework. Other
events, that have been generated before, won't be delivered to handlers
until a `Start` event has been received.

The easiest way to send the `Start` event to the application is shown
in the code: using the convenience method from
[Components](latest-release/javadoc/index.html?org/jgrapes/core/Components.html)[^finalS].

[^finalS]: Note the "s" at the end.


*ToDo*

---
