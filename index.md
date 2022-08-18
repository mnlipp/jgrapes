---
layout: default
title: JGrapes by mnlipp
description: Introductory description of the JGrapes event driven component framework for Java
---

Welcome to JGrapes
==================

JGrapes is an event driven (message driven) component framework written 
in Java. It includes support for asynchronous I/O and can be used to 
build reactive systems. It was inspired by the 
["circuits framework"](https://pypi.org/project/circuits/#description) 
written for Python.

Built on the core, the JGrapes framework also includes some complex 
components such as an HTTP server and a web console provided as component
libraries. The framework makes full use of the Java 11 SE environment. 
The core has no dependencies on additional libraries. The component 
libraries extending the core have as few dependencies on additional 
libraries as possible.

The documentation of the framework and its components can be found
in the <a href="latest-release/javadoc/index.html" target="_top">JavaDoc</a>. 
Make sure to check the package descriptions. They tend to
be overlooked because they come after the sometimes rather lengthy list
of packages or classes (though the standard layout *does* put a link
to the description at the top of the page). To get started, read
the framework's architectural description on the
<a href="latest-release/javadoc/index.html" target="_top">overview page</a>. 
This provides an introduction to the concepts, the main classes and 
links to the details.

A Web Console built on top of the JGrapes framework is described 
on [a page of its own](WebConsole.html).

All jars have the required information in `META-INF/MANIFEST.MF`
to use them as (library) bundles in an OSGi environment, without 
introducing a dependency on this environment. Built on top 
of these libraries, there are some 
components that fully integrate with OSGi (often just adapters for 
the basic components). Because they are built in an independant
workspace, they have their own 
<a href="javadoc-osgi/index.html" target="_top">Javadoc</a>.

