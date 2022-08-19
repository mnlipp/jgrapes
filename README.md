JGrapes
=======

See the [project's home page](https://mnlipp.github.io/jgrapes/) for a
detailed description of the framework.

[![Build Status](https://github.com/mnlipp/jgrapes/workflows/Java%20CI/badge.svg)](https://github.com/mnlipp/jgrapes/actions) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/917a465504c444bd8adcb46eb000aaa9)](https://www.codacy.com/gh/mnlipp/jgrapes/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mnlipp/jgrapes&amp;utm_campaign=Badge_Grade)

| Package | Download |
| ------- | -------- |
| core    | [![Download](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.core.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.core%22)
| util    | [![Download](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.util.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.util%22)
| io      | [![Download](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.io.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.io%22)
| http    | [![Download](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.http.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.http%22)

This repository comprises the sources for jars that provide the basic
packages (org.jgrapes.core, ...util, ...io etc.). The jars have augmented
manifests that allow them to be used as OSGi bundles without wrapping, 
but they do not depend in any way on the OSGi framework.

Short Overview
--------------

JGrapes is an event driven (message driven) component framework written 
in Java. It includes support for asynchronous I/O and can be used to 
build reactive systems. It was inspired by the 
["circuits framework"](https://pypi.org/project/circuits/#description) 
written for Python.

This repository is used to maintain the library with 
the interfaces and classes that provide the
event handling features of the core framework.
Some additional libraries built on top of the core which
support asynchronous I/O and provide some more complex components 
such as an HTTP server are also maintained in this repository.

A web console that uses these basic libraries as a foundation
is maintained as a separate 
[JGrapes Web Console](https://github.com/mnlipp/jgrapes-webconsole)
project.

The JGrapes OSGi components (that depend on the OSGi framework and 
provide JGrapes based OSGi services) can also be found in a
[repository of their own](https://github.com/mnlipp/jgrapes-osgi).

See the [project's home page](https://mnlipp.github.io/jgrapes/) for a
detailed description of the framework.

Running
-------

JGrapes requires Java 11 SE or newer. Binaries are currently made
available at Maven Central.

```gradle
repositories {
        mavenCentral()
}

dependencies {
        compile 'org.jgrapes:org.jgrapes.PACKAGE:X.Y.Z'
}
```

(See badge above for the latest version.)

Building
--------

The libraries can be built with `gradle build`. For working with 
the project in Eclipse run `gradle eclipse` before importing the 
project. 

If you want to use 
[buildship](https://projects.eclipse.org/projects/tools.buildship),
import the project as "Gradle / Existing Gradle Project". Should you
encounter the (in)famous 
["sync problem"](https://github.com/eclipse/buildship/issues/478),
simply restart Eclipse.
