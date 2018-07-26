package com.github.j5ik2o.reactive.memcached
import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import cats.implicits._
import scala.collection.mutable.ListBuffer

class HashRingConnectionSpec extends AbstractActorSpec(ActorSystem("HashRingConnectionSpec")) {

  val connectionPools: ListBuffer[MemcachedConnectionPool[Task]] = ListBuffer.empty[MemcachedConnectionPool[Task]]

  var secondServer: MemcachedTestServer = _

  var hashRingConnection: HashRingConnection = _

  val client = MemcachedClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    secondServer = new MemcachedTestServer()
    secondServer.start()
    val p1 = MemcachedConnectionPool.ofSingleRoundRobin(
      sizePerPeer = 2,
      peerConfig = PeerConfig(
        remoteAddress = new InetSocketAddress("127.0.0.1", memcachedTestServer.getPort),
        backoffConfig = BackoffConfig(maxRestarts = 1)
      ),
      newConnection = MemcachedConnection(_, _)
    )
    val p2 = MemcachedConnectionPool.ofSingleRoundRobin(
      sizePerPeer = 2,
      peerConfig = PeerConfig(
        remoteAddress = new InetSocketAddress("127.0.0.1", secondServer.getPort),
        backoffConfig = BackoffConfig(maxRestarts = 1)
      ),
      newConnection = MemcachedConnection(_, _)
    )
    connectionPools.append(p1, p2)

    hashRingConnection = HashRingConnection(connectionPools.result)
  }

  override protected def afterAll(): Unit = {
    connectionPools.foreach(_.dispose())
    secondServer.stop()
    super.afterAll()
  }

  "HashRingConnection" - {
    "set & get" in {
      val key1   = UUID.randomUUID().toString
      val value1 = UUID.randomUUID().toString
      val key2   = UUID.randomUUID().toString
      val value2 = UUID.randomUUID().toString
      val result = (for {
        _  <- client.set(key1, value1)
        r1 <- client.get(key1)
        _  <- client.set(key2, value2)
        r2 <- client.get(key2)
      } yield (r1, r2)).run(hashRingConnection).runAsync.futureValue

      result._1.map(_.value) shouldBe Some(value1)
      result._2.map(_.value) shouldBe Some(value2)
    }
  }
}
