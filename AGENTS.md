# JGrapes — Agent Instructions

## Build System

The project uses **jdbld**, not Gradle or Maven. The README mentions
Gradle but that is stale.

| Command | Effect |
|---------|--------|
| `./jdbld build` | Build all packages |
| `./jdbld test`  | Run all tests |
| `./jdbld eclipse` | Generate Eclipse project files |
| `./jdbld javadoc` | Generate Javadoc |
| `./jdbld baseline` | Check OSGi API baseline |
| `./jdbld runGreeter` | Run Greeter example |
| `./jdbld runEchoUntilQuit` | Run echo console example |
| `./jdbld runEchoServer` | Run TCP echo server example |
| `./jdbld runHttpServerDemo` | Run HTTP server demo |

Build configuration lives in `_jdbld/src/jdbld/*.java`. Each package
has a corresponding class (`Core.java`, `Util.java`, etc.) that declares
dependencies and test setup.

## Prerequisites

- **Java 21+** (project uses virtual threads; `--release 21` compiler flag)
- `jdbld` downloads its own runner jar automatically on first use
- CI uses OpenJDK 25; local dev works with any JDK 21+
- Set `JAVA_HOME` or rely on system Java

## Package Structure

Monorepo with 6 library packages and 1 examples package:

| Directory | Bundle | Depends on |
|-----------|--------|------------|
| `org.jgrapes.core` | Core framework (events, actors) | — |
| `org.jgrapes.util` | Utilities (config, YAML, TOML) | core |
| `org.jgrapes.io` | Async I/O, JSON serialization | core, util |
| `org.jgrapes.http` | HTTP server/client | core, io |
| `org.jgrapes.http.freemarker` | Freemarker template integration | http |
| `org.jgrapes.mail` | Email support | core, util, io |
| `examples` | Demo applications | http, mail |

Each library directory has `src/` (main code), `test/` (tests), and
`bnd.bnd` (OSGi manifest instructions).

## OSGi / bnd

Each package has a `bnd.bnd` file controlling OSGi bundle metadata.
The root `cnf/build.bnd` sets shared defaults
(`-javac.source/target: 21`, sources jar, etc.). Packages produce jars
with augmented manifests usable as OSGi bundles without wrapping, but
have no OSGi runtime dependency.

## Code Style

- 4-space indentation for Java (`.editorconfig`)
- Checkstyle config: root `checkstyle.xml`
  (rules at module level: `*/.checkstyle`)
- PMD rules: root `ruleset.xml`
  (excludes `*/test/` and `*/node_modules/`)
- Eclipse: run `./jdbld eclipse` after clone to generate project metadata

## Testing

- Tests use JUnit 4 and JUnit 5 (via vintage engine)
- Each package's `*Test` inner class in `_jdbld/src/jdbld/`
  declares test dependencies
- Test sources live in `*/test/`, test resources in `*/test-resources/`
