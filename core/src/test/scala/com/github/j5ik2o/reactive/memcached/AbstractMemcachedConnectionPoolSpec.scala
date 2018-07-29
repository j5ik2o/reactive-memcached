package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import cats.data.NonEmptyList
import com.github.j5ik2o.reactive.memcached.command.{ SetRequest, _ }
import monix.eval.Task

import scala.concurrent.duration._
import cats.implicits._
import monix.execution.Scheduler

abstract class AbstractMemcachedConnectionPoolSpec(systemName: String)
    extends AbstractActorSpec(ActorSystem(systemName)) {

  private var pool: MemcachedConnectionPool[Task] = _

  protected def createConnectionPool(peerConfigs: NonEmptyList[PeerConfig]): MemcachedConnectionPool[Task]

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val peerConfigs = NonEmptyList.of(
      PeerConfig(
        remoteAddress = new InetSocketAddress("127.0.0.1", memcachedTestServer.getPort),
        backoffConfig = BackoffConfig(maxRestarts = 1)
      )
    )
    pool = createConnectionPool(peerConfigs)
  }

  override protected def afterAll(): Unit = {
    pool.dispose()
    super.afterAll()
  }

  s"MemcachedConnectionPool_${UUID.randomUUID()}" - {
    "set & get" in {
      implicit val scheduler = Scheduler(system.dispatcher)
      val key1               = UUID.randomUUID().toString
      val key2               = UUID.randomUUID().toString
      val value              = UUID.randomUUID().toString
      val result = (for {
        _   <- ConnectionAutoClose(pool)(_.send(SetRequest(UUID.randomUUID(), key1, value, 10 seconds)))
        _   <- ConnectionAutoClose(pool)(_.send(SetRequest(UUID.randomUUID(), key2, value, 10 seconds)))
        gr1 <- ConnectionAutoClose(pool)(_.send(GetRequest(UUID.randomUUID(), key1)))
        gr2 <- ConnectionAutoClose(pool)(_.send(GetRequest(UUID.randomUUID(), key2)))
        dr  <- ConnectionAutoClose(pool)(_.send(DeleteRequest(UUID.randomUUID(), key1)))
      } yield (gr1, gr2, dr)).run.runAsync.futureValue
      result._1.asInstanceOf[GetSucceeded].value.get.value shouldBe value
      result._2.asInstanceOf[GetSucceeded].value.get.value shouldBe value
      result._3.isInstanceOf[DeleteSucceeded] shouldBe true
    }
  }

}
