---
layout: default
title: JGrapes Web Console Introduction
---

JGrapes Web Console
===================

The server side of the web console consists of several JGrapes components
that drive a single page application (SPA) on the server. The design
is highly modular and allows the adaption of the web console to 
different purposes.

<img src="WebConsole-pic1.png" width="75%" alt="Web Console Demo">

A JGrapes Web Console consists &mdash;from the user's point of view&mdash; 
of a fixed frame with configurable content. The frame provides some 
means to add content  (typically by using a dropdown menu) and to 
configure global settings  such as the locale.

The content of the frame is provided by web console display components 
or "conlets" for short. These components typically provide a summary
or preview display that can be put on an overview panel in a dashboard
style and a large view that is supposed to fill the complete frame.

Tabs or a menu in a side bar can be used to switch between
the overview panel(s) and the large views of the different conlets. 

The architecture of the server side is explained in detail in the
[package description of the base component](http://127.0.0.1:4000/javadoc-webconsole/org/jgrapes/webconsole/base/package-summary.html#package.description).
The additional information provided here focuses on the SPA 
in the browser and on how to build your own console and additional conlets.

*To be continued*
