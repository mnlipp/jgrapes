---
layout: default
title: JGrapes Logging
---

Logging
=======

Debugging an event driven application is known to be difficult, mainly 
because the commonly used "step into" is not always available. If you
want to see what happens with an event that a handler (that you
are debugging) generates, there is no way but to anticipate the handler
that will be invoked for this event and set another break point.

To simplify the a task to a certain degree, JGrapes provides exhaustive
logging of events with a carefully designed format. Below is the
log of

 * starting the TCP Echo Application,
 * connecting a client,
 * sending data from the client to the server,
 * sending back the data to the client, and
 * closing the connection on the client side.
 

```
2018-10-17 P1: Attached#1 [ROOT <―― EchoServer#1, channels=[network i/o]] (unhandled)
2018-10-17 P1: Attached#2 [EchoServer#1 <―― NioDispatcher#1, channels=[network i/o, NioDispatcher#1]] (unhandled)
2018-10-17 P1: Attached#3 [NioDispatcher#1 <―― TcpServer#1, channels=[NioDispatcher#1, network i/o]] (unhandled)
2018-10-17 P1: Start#1 [channels=[BROADCAST]] >> Handler [method=NioDispatcher#1.onStart, filter=Scope [handledEvents=[Start], handledChannels=[NioDispatcher#1]], priority=0]
2018-10-17 P1: Start#1 [channels=[BROADCAST]] >> Handler [method=TcpServer#1.onStart, filter=Scope [handledEvents=[Start], handledChannels=[TcpServer#1, network i/o]], priority=0]
2018-10-17 P1: NioRegistration#1 [channels=[BROADCAST]] >> Handler [method=NioDispatcher#1.onNioRegistration, filter=Scope [handledEvents=[NioRegistration], handledChannels=[NioDispatcher#1]], priority=0]
2018-10-17 P1: Completed(NioRegistration#1) [channels=[TcpServer#1]] >> Handler [method=TcpServer#1.onRegistered, filter=Scope [handledEvents=[Completed], handledChannels=[TcpServer#1]], priority=0]
2018-10-17 P1: Ready#1 [/0:0:0:0:0:0:0:0:8888, channels=[network i/o]] (unhandled)
2018-10-17 P1: Started(Start#1) [channels=[BROADCAST]] (unhandled)
2018-10-17 Thread main is waiting, 2 generators registered: [EventProcessor#1 [queue=[]], NioDispatcher#1]
2018-10-17 P1: NioRegistration#2 [channels=[BROADCAST]] >> Handler [method=NioDispatcher#1.onNioRegistration, filter=Scope [handledEvents=[NioRegistration], handledChannels=[NioDispatcher#1]], priority=0]
2018-10-17 P1: Completed(NioRegistration#2) [channels=[TcpServer#1]] >> Handler [method=TcpServer#1.onRegistered, filter=Scope [handledEvents=[Completed], handledChannels=[TcpServer#1]], priority=0]
2018-10-17 P2: Accepted#1 [/127.0.0.1:8888 <― /127.0.0.1:38794, channels=[network i/o{TcpChannelImpl#1}], secure=false] (unhandled)
2018-10-17 P2: Input#1 [channels=[network i/o{TcpChannelImpl#1}],size=8,eor=false] >> Handler [method=EchoServer#1.onRead, filter=Scope [handledEvents=[Input], handledChannels=[network i/o, EchoServer#1]], priority=0]
2018-10-17 P3: Output#1 [channels=[network i/o{TcpChannelImpl#1}],size=8,eor=false] >> Handler [method=TcpServer#1.onOutput, filter=Scope [handledEvents=[Output], handledChannels=[TcpServer#1, network i/o]], priority=0]
2018-10-17 P2: RunnableActionEvent#1 [channels=[]] >> Handler [method=ActionExecutor#1.execute, filter=wildcard, priority=0]
2018-10-17 P4: HalfClosed#1 [channels=[network i/o{TcpChannelImpl#1}]] (unhandled)
2018-10-17 P2: Closed#1 [channels=[network i/o{TcpChannelImpl#1}]] (unhandled)
```

It should be easy to interpret the log, knowing the structure of the 
application. The *Pn* at the beginning of each line indicates the
event pipeline that invokes the handler.

You can also see that there are many additional events aside from input
and ouput that we didn't care about in the example application. Of course,
you can add handlers for these events and e.g. print a mesage for each
new client connected to the server, or track the number of clients etc.
