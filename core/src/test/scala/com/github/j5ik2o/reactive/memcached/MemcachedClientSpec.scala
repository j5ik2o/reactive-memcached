package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import monix.execution.Scheduler.Implicits.global
import cats.implicits._

class MemcachedClientSpec extends AbstractActorSpec(ActorSystem("MemcachedClientSpec")) {

  var connection: MemcachedConnection = _

  val client = MemcachedClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    connection = MemcachedConnection(
      PeerConfig(new InetSocketAddress("127.0.0.1", testServer.getPort),
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
  }

}
