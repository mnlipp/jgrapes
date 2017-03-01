An event driven component framework.

JGrapes
=======

JGrapes is an event driven component framework. It consists of
three packages.

![Package hierarchy](package-hierarchy.svg)

`org.jgrapes.core`
: This package provides the basic mechanisms for defining
    components and for handling events. See the
    <a href="org/jgrapes/core/package-summary.html">package description</a>
    for details.

@startuml package-hierarchy.svg

package org.jgrapes {
    package org.jgrapes.core {
    }

    package org.jgrapes.io {
    }

    package org.jgrapes.http {
    }
}

org.jgrapes.core <.. org.jgrapes.io
org.jgrapes.core <.. org.jgrapes.http
org.jgrapes.io <.right. org.jgrapes.http

@enduml