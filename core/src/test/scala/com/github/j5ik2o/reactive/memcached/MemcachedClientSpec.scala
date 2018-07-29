package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import cats.implicits._
import monix.execution.Scheduler.Implicits.global

class MemcachedClientSpec extends AbstractActorSpec(ActorSystem("MemcachedClientSpec")) {

  var connection: MemcachedConnection = _

  val client = MemcachedClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    connection = MemcachedConnection(
      PeerConfig(new InetSocketAddress("127.0.0.1", memcachedTestServer.getPort),
                 backoffConfig = BackoffConfig(maxRestarts = 1)),
      None
    )
  }

  override protected def afterAll(): Unit = {
    connection.shutdown()
    super.afterAll()
  }

  "MemcachedClient" - {
    "set & get" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _ <- client.set(key, value)
        r <- client.get(key)
      } yield r).run(connection).runAsync.futureValue

      result.map(_.value) shouldBe Some(value)
    }
    "set & add" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _   <- client.set(key, value)
        gr1 <- client.get(key)
        _   <- client.add(key, value)
        gr2 <- client.get(key)
      } yield gr2).run(connection).runAsync.futureValue

      result.map(_.value) shouldBe Some(value)
    }
    "set & gets" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _ <- client.set(key, value)
        r <- client.gets(key)
      } yield r).run(connection).runAsync.futureValue

      result.map(_.value) shouldBe Some(value)
      result.flatMap(_.casUnique) should not be empty
    }
    "cas & gets" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _   <- client.set(key, value)
        gr1 <- client.gets(key)
        _   <- client.cas(key, value, gr1.get.casUnique.get)
        gr2 <- client.gets(key)
      } yield gr2).run(connection).runAsync.futureValue

      result.map(_.value) shouldBe Some(value)
      result.flatMap(_.casUnique) should not be empty
    }
  }

}
