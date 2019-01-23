package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.ActorSystem
import cats.implicits._

import scala.concurrent.duration._

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

      client.get(key).run(connection).runToFuture.futureValue shouldBe None

      val result1 = (for {
        _ <- client.set(key, value)
        r <- client.get(key)
      } yield r).run(connection).runToFuture.futureValue

      result1.map(_.value) shouldBe Some(value)

      val result2 = (for {
        _  <- client.set(key, UUID.randomUUID().toString)
        r1 <- client.get(key)
        _  <- client.set(key, UUID.randomUUID().toString)
        r2 <- client.get(key)
      } yield (r1, r2)).run(connection).runToFuture.futureValue

      result2._1.map(_.value) should not be result2._2.map(_.value)

    }
    "add" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result1 = (for {
        _  <- client.add(key, value)
        gr <- client.get(key)
      } yield gr).run(connection).runToFuture.futureValue

      result1.map(_.value) shouldBe Some(value)

      client.add(key, value).run(connection).runToFuture.futureValue shouldBe 0
    }
    "set & gets" in {
      val key   = UUID.randomUUID().toString
      val value = UUID.randomUUID().toString
      val result = (for {
        _  <- client.set(key, value)
        gr <- client.gets(key)
      } yield gr).run(connection).runToFuture.futureValue

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
      } yield gr2).run(connection).runToFuture.futureValue

      result.map(_.value) shouldBe Some(value)
      result.flatMap(_.casUnique) should not be empty
    }
    "touch" in {
      val expire = 3 * timeFactor seconds
      val key    = UUID.randomUUID().toString
      val value  = UUID.randomUUID().toString
      (for {
        _ <- client.set(key, value, expire)
        _ <- client.touch(key, expire * 5)
      } yield ()).run(connection).runToFuture.futureValue
      Thread.sleep(expire.toMillis)
      client.get(key).run(connection).runToFuture.futureValue.map(_.value) shouldBe Some(value)
    }
    "gat" in {
      val expire = 3 * timeFactor seconds
      val key    = UUID.randomUUID().toString
      val value  = UUID.randomUUID().toString
      val result = (for {
        _  <- client.set(key, value, expire)
        gr <- client.gat(key, expire * 5)
      } yield gr).run(connection).runToFuture.futureValue
      Thread.sleep(expire.toMillis)
      client.get(key).run(connection).runToFuture.futureValue.map(_.value) shouldBe Some(value)
      result.map(_.value) shouldBe Some(value)
      result.flatMap(_.casUnique).isEmpty shouldBe true
    }
    "gats" in {
      val expire = 3 * timeFactor seconds
      val key    = UUID.randomUUID().toString
      val value  = UUID.randomUUID().toString
      val result = (for {
        _  <- client.set(key, value, expire)
        gr <- client.gats(key, expire * 5)
      } yield gr).run(connection).runToFuture.futureValue
      Thread.sleep(expire.toMillis)
      client.get(key).run(connection).runToFuture.futureValue.map(_.value) shouldBe Some(value)
      result.map(_.value) shouldBe Some(value)
      result.flatMap(_.casUnique).nonEmpty shouldBe true
    }
  }

}
