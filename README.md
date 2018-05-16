JGrapes
=======

[![Build Status](https://travis-ci.org/mnlipp/jgrapes.svg?branch=master)](https://travis-ci.org/mnlipp/jgrapes) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/42ed1920969d4b878d7ce80c08141a85)](https://www.codacy.com/app/mnlipp/jgrapes?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mnlipp/jgrapes&amp;utm_campaign=Badge_Grade)

| Package | Maven |
| ------- | ----- |
| core    | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.core.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.core%22)
| util    | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.util.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.util%22)
| io      | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.io.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.io%22)
| http    | [![Maven Central](https://img.shields.io/maven-central/v/org.jgrapes/org.jgrapes.http.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22org.jgrapes.http%22)

See the [project's home page](https://mnlipp.github.io/jgrapes/).

This repository comprises the sources for jars that provide the basic
packages (org.jgrapes.core, ...util, ...io etc.). The jars have augmented
manifests that allow them to be used without wrapping as OSGi bundles, 
but they do not depend in any way on the OSGi framework.

JGrapes requires Java 8 SE. Binaries are currently made
available at maven central.

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

Additional JGrapes components
-----------------------------

A portal built on top of the basic libraries is maintained as a
seperate [JGrapes Portal](https://github.com/mnlipp/jgrapes-portal) project.

The JGrapes OSGi components (that depend on the OSGi framework and 
provide JGrapes based OSGi services) can also be found in a
[repository of their own](https://github.com/mnlipp/jgrapes-osgi). 
