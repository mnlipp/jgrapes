An event driven component framework.

JGrapes
=======

JGrapes is an event driven component framework. It consists of
several packages that built on each other. Each package is available
as a jar file.

<object type="image/svg+xml" data="package-hierarchy.svg">Package hierarchy</object>

`org.jgrapes.core`
: This package provides the basic mechanisms for defining
    components and for handling events. Make sure to read the
    <a href="org/jgrapes/core/package-summary.html#package.description">package description</a>
    first. It explains the architecture of the framework and its main elements. 

`org.jgrapes.io`
: Input/Output related extensions for the framework. See the
    <a href="org/jgrapes/io/package-summary.html#package.description">package description</a>
    for details. 

`org.jgrapes.http`
: Components for building HTTP servers. See the
    <a href="org/jgrapes/http/package-summary.html#package.description">package description</a>
    for details. 

`org.jgrapes.portal`
: Components for building portals. See the
    <a href="org/jgrapes/portal/package-summary.html#package.description">package description</a>
    for details. 
    
@startuml package-hierarchy.svg
skinparam svgLinkTarget _parent

package org.jgrapes {
    package org.jgrapes.core [[org/jgrapes/core/package-summary.html#package.description]] {
    }

    package org.jgrapes.util [[org/jgrapes/util/package-summary.html#package.description]] {
    }

    package org.jgrapes.io [[org/jgrapes/io/package-summary.html#package.description]] {
    }

    package org.jgrapes.http [[org/jgrapes/http/package-summary.html#package.description]] {
    }

    package org.jgrapes.portal [[org/jgrapes/portal/package-summary.html#package.description]] {
    }

    package "org.jgrapes.portal.themes.*" {
    }
}

org.jgrapes.core <.. org.jgrapes.io
org.jgrapes.core <.. org.jgrapes.util
org.jgrapes.core <.. org.jgrapes.http
org.jgrapes.io <.right. org.jgrapes.http
org.jgrapes.http <.. org.jgrapes.portal
org.jgrapes.portal <.. "org.jgrapes.portal.themes.*"


@enduml