package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import cats.data.NonEmptyList
import com.github.j5ik2o.reactive.memcached.command.{ GetRequest, GetSucceeded, SetRequest }
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._

class MemcachedConnectionSpec extends AbstractActorSpec(ActorSystem("MemcachedConnectionSpec")) {
  var connection: MemcachedConnection = _
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
  "MemcachedConnectionSpec" - {
    "set & get" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _ <- connection.send(SetRequest(UUID.randomUUID(), key, 0, 10 seconds, value))
        r <- connection.send(GetRequest(UUID.randomUUID(), NonEmptyList.of(key)))
      } yield r).runAsync.futureValue
      result.asInstanceOf[GetSucceeded].value.get.value shouldBe value
    }
  }

}
