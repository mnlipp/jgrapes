---
layout: default
title: JGrapes Hello World Example
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
event. This event triggers the event processing by the framework. Other
events, that have been fired before the `Start` event, won't be delivered 
to handlers until the `Start` event has been fired.

The easiest way to fire the `Start` event at the application is shown
in the code: invoking the convenience method from
[Components](latest-release/javadoc/index.html?org/jgrapes/core/Components.html)[^finalS].
This method fires a `Start` event on the broadcast channel of the given 
application and waits until it is processed. 

[^finalS]: Note the "s" at the end.

In our application, the `Start` event is handled by the method `onStart`
of the class `Greeter`, which simply outputs "`Hello World!`". The method
is marked as a handler method using the 
[@Handler](latest-release/javadoc/index.html?org/jgrapes/core/annotation/Handler.html)
annotation. The annotation is evaluated when a new `Component` is created.
By reflection, it finds that the annotated method accepts an event of type
`Start` as first parameter and therefore registers it as handler for such an
event. 

---
