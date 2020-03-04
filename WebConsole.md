---
layout: default
title: JGrapes Web Console Introduction
tocTitle: JGrapes Web Console
---

## Overview

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

## SPA frame

The SPA frame is provided by a class derived from 
[`ConsoleWeblet`](javadoc-webconsole/index.html?org/jgrapes/webconsole/base/ConsoleWeblet.html).
If you like (or can live with) the [Freemarker](https://freemarker.apache.org/)
template engine, you should use
[`FreeMarkerConsoleWeblet`](javadoc-webconsole/index.html?org/jgrapes/webconsole/base/freemarker/FreeMarkerConsoleWeblet.html)
as base class. Using the latter class, all you have to do is to [implement
the constructor](javadoc-webconsole/src-html/org/jgrapes/webconsole/vuejs/VueJsConsoleWeblet.html#line.36)
and provide the required 
[templates](https://github.com/mnlipp/jgrapes-webconsole/tree/master/org.jgrapes.webconsole.vuejs/resources/org/jgrapes/webconsole/vuejs).

## Dynamic modularity for the SPA

Web applications are nowadays mostly developed as modular application.
However, the modularity focuses on the code base. The various modules
are then bundled by some tool and provided as monolithic resources.
Optimization steps in this process may even remove
JavaScript code from libraries if analysis shows that it isn't invoked,
thus making libraries only partially available.

The JGrapes web console objectives include the support for dynamic
addition of conlets. Adding a conlet to a running system may require
adding resources on the server side as well as in the SPA. The server
side can easily be handled by a framework such as OSGi. Support in the
SPA turns out to be a bit more difficult to implement.

### Dynamically adding CSS

This can be implemented by adding additional `link` nodes to the
`head` node in the DOM with JavaScript. The added links cause the browser to 
load the respective style sheets. Style sheets are applied as they become 
available, so the asynchronous loading may, in the worst case, result in a 
visible change of the pages' appearance after its initial display.

Adding `link` nodes in the SPA is triggered on the server side by
firing 
[`AddPageResources`](javadoc-webconsole/index.html?org/jgrapes/webconsole/base/events/AddPageResources.html)
events.

### Dynamically adding JavaScript

If everybody used ES6 modules, this wouldn't be a problem either. 
An `import` statement in ES6 JavaScript causes the interpreter to 
block until the required module has been loaded. If ES6 modules
aren't used, we have to resort to adding a `script` node to the
`head` node in the DOM. In this case the application has to make sure
that required resources are loaded before the requiring JavaScript.

In the JGrapes web console, the necessary dependency tracking and
ordered insertion of the `script` nodes is handled by a class that
obtains the required information from `ScriptResource` instances
as described in 
[`AddPageResources`](javadoc-webconsole/index.html?org/jgrapes/webconsole/base/events/AddPageResources.html).

## Styling Conlets

At least for simple conlets, it should be possible to combine them with
differently styled consoles. This requirement implies that conlets are
styled independent of a particular CSS framework.

Traditionally, CSS frame works are "invasive" in the sense that the 
framework's classes are spread all over your HTML.

*To be continued*
