# reactive-mecached

[![CircleCI](https://circleci.com/gh/j5ik2o/reactive-memached/tree/master.svg?style=svg)](https://circleci.com/gh/j5ik2o/reactive-memached/tree/master)

Akka-Stream based Memcached Client for Scala

## Concept

- Transport is akka-stream 2.5.x.
- Response parser is fastparse.

## Installation

Add the following to your sbt build (Scala 2.11.x, 2.12.x):

### Release Version

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.github.j5ik2o" %% "reactive-memcached-core" % "1.0.6"
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "com.github.j5ik2o" %% "reactive-memached-core" % "1.0.7-SNAPSHOT"
```

## Usage

### Non connection pooling

```scala
```

### Connection pooling

```scala
```

### Master & Slaves aggregate connection

```scala
```

## License

MIT License / Copyright (c) 2018 Junichi Kato

