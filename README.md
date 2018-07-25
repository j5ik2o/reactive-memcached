# [WIP] reactive-mecached

[![CircleCI](https://circleci.com/gh/j5ik2o/reactive-memached/tree/master.svg?style=svg)](https://circleci.com/gh/j5ik2o/reactive-memached/tree/master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0f8d5414b1da449d85299daa9934c899)](https://www.codacy.com/app/j5ik2o/reactive-memached?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=j5ik2o/reactive-memached&amp;utm_campaign=Badge_Grade)

Akka-Stream based Memcached Client for Scala

## Concept

- Transport is akka-stream 2.5.x.
- Response parser is fastparse.

## Installation

Add the following to your sbt build (Scala 2.11.x, 2.12.x):

### Release Version

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.github.j5ik2o" %% "reactive-memcached-core" % "1.0.0"
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "com.github.j5ik2o" %% "reactive-memached-core" % "1.0.0-SNAPSHOT"
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

