package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import cats.implicits._
import com.github.j5ik2o.reactive.memcached.command._
import monix.execution.Scheduler

import scala.concurrent.duration._

class MemcachedConnectionSpec extends AbstractActorSpec(ActorSystem("MemcachedConnectionSpec")) {

  var connection: MemcachedConnection = _

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

  "MemcachedConnectionSpec" - {

    "set & get" in {
      val key1  = UUID.randomUUID().toString
      val key2  = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _   <- connection.send(SetRequest(UUID.randomUUID(), key1, value, 10 seconds))
        _   <- connection.send(SetRequest(UUID.randomUUID(), key2, value, 10 seconds))
        gr1 <- connection.send(GetRequest(UUID.randomUUID(), key1))
        gr2 <- connection.send(GetRequest(UUID.randomUUID(), key2))
        dr  <- connection.send(DeleteRequest(UUID.randomUUID(), key1))
      } yield (gr1, gr2, dr)).runAsync.futureValue
      result._1.asInstanceOf[GetSucceeded].value.get.value shouldBe value
      result._2.asInstanceOf[GetSucceeded].value.get.value shouldBe value
      result._3.isInstanceOf[DeleteSucceeded] shouldBe true
    }

    "add" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      connection
        .send(AddRequest(UUID.randomUUID(), key, value))
        .runAsync
        .futureValue
        .isInstanceOf[AddSucceeded] shouldBe true
      connection
        .send(AddRequest(UUID.randomUUID(), key, value))
        .runAsync
        .futureValue
        .isInstanceOf[AddNotStored] shouldBe true
    }

    "replace" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      connection
        .send(ReplaceRequest(UUID.randomUUID(), key, value))
        .runAsync
        .futureValue
        .isInstanceOf[ReplaceNotStored] shouldBe true
      connection
        .send(SetRequest(UUID.randomUUID(), key, value))
        .runAsync
        .futureValue
        .isInstanceOf[SetSucceeded] shouldBe true
      connection
        .send(ReplaceRequest(UUID.randomUUID(), key, value))
        .runAsync
        .futureValue
        .isInstanceOf[ReplaceSucceeded] shouldBe true
    }

    "inc & dec" in {
      val key   = UUID.randomUUID().toString
      val value = 1L
      val result = (for {
        _  <- connection.send(SetRequest(UUID.randomUUID(), key, 0, 10 seconds))
        ir <- connection.send(IncrementRequest(UUID.randomUUID(), key, value))
        gr <- connection.send(GetRequest(UUID.randomUUID(), key))
        dr <- connection.send(DecrementRequest(UUID.randomUUID(), key, value))
      } yield (ir, gr, dr)).runAsync.futureValue
      result._1.asInstanceOf[IncrementSucceeded].value shouldBe 1
      result._2.asInstanceOf[GetSucceeded].value.get.value.toInt shouldBe value
      result._3.asInstanceOf[DecrementSucceeded].value shouldBe 0
    }

  }

}
