# reactive-mecached

[![CircleCI](https://circleci.com/gh/j5ik2o/reactive-memcached/tree/master.svg?style=svg)](https://circleci.com/gh/j5ik2o/reactive-memcached/tree/master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.j5ik2o/reactive-memcached-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.j5ik2o/reactive-memcached-core_2.12)
[![Scaladoc](http://javadoc-badge.appspot.com/com.github.j5ik2o/reactive-memcached-core_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.github.j5ik2o/reactive-memcached-core_2.12/com/github/j5ik2o/reactive/memcached/index.html?javadocio=true)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0f8d5414b1da449d85299daa9934c899)](https://www.codacy.com/app/j5ik2o/reactive-memached?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=j5ik2o/reactive-memached&amp;utm_campaign=Badge_Grade)
[![License: MIT](http://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)

Akka-Stream based Memcached Client for Scala

## Concept

- Transport is akka-stream 2.5.x.
- Response parser is fastparse.
- monix.eval.Task support.

## Support Protocol

https://github.com/memcached/memcached/blob/master/doc/protocol.txt

## Installation

Add the following to your sbt build (Scala 2.11.x, 2.12.x):

### Release Version

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.github.j5ik2o" %% "reactive-memcached-core" % "1.0.4"
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "com.github.j5ik2o" %% "reactive-memcached-core" % "1.0.5-SNAPSHOT"
```

## Usage

### Non connection pooling

```scala
import monix.execution.Scheduler.Implicits.global

implicit val system = ActorSystem()

val peerConfig = PeerConfig(remoteAddress = new InetSocketAddress("127.0.0.1", 6379))
val connection = MemcachedConnection(peerConfig)
val client = MemcachedClient()

val result = (for{
  _ <- client.set("foo", "bar")
  r <- client.get("foo")
} yield r).run(connection).runAsync

println(result) // bar
```

### Connection pooling

```scala
import monix.execution.Scheduler.Implicits.global

implicit val system = ActorSystem()

val peerConfig = PeerConfig(remoteAddress = new InetSocketAddress("127.0.0.1", 6379))
val pool = MemcachedConnectionPool.ofSingleRoundRobin(sizePerPeer = 5, peerConfig, RedisConnection(_)) // powered by RoundRobinPool
val connection = MemcachedConnection(connectionConfig)
val client = MemcachedClient()

// Fucntion style
val result1 = pool.withConnectionF{ con =>
  (for{
    _ <- client.set("foo", "bar")
    r <- client.get("foo")
  } yield r).run(con) 
}.runAsync

println(result1) // bar

// Monadic style
val result2 = (for {
  _ <- ConnectionAutoClose(pool)(client.set("foo", "bar").run)
  r <- ConnectionAutoClose(pool)(client.get("foo").run)
} yield r).run().runAsync

println(result2) // bar
```

if you want to use other pooling implementation, please select from the following modules.

- reactive-memcached-pool-commons (commons-pool2)
- reactive-memcached-pool-scala (scala-pool)
- reactive-memcached-pool-fop (fast-object-pool)
- reactive-memcached-pool-stormpot (stormpot)

### Hash ring connection

```scala
import monix.execution.Scheduler.Implicits.global

implicit val system = ActorSystem()

val peerConfigs = Seq(
  PeerConfig(remoteAddress = new InetSocketAddress("127.0.0.1", 6380)),
  PeerConfig(remoteAddress = new InetSocketAddress("127.0.0.1", 6381)),
  PeerConfig(remoteAddress = new InetSocketAddress("127.0.0.1", 6382))
)

val connection = HashRingConnection(peerConfigs)

val client = MemcachedClient()

val result = (for{
  _ <- client.set("foo", "bar") // write to master
  r <- client.get("foo")        // read from any slave
} yield r).run(connection).runAsync

println(result) // bar
```

## License

MIT License / Copyright (c) 2018 Junichi Kato

