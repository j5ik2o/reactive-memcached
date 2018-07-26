package com.github.j5ik2o.reactive.memcached
import akka.routing.DefaultResizer
import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

class MemcachedConnectionPoolSpec extends AbstractMemcachedConnectionPoolSpec("MemcachedConnectionPoolSpec") {
  override protected def createConnectionPool(
      peerConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    MemcachedConnectionPool.ofMultipleRoundRobin(sizePerPeer = 10,
                                                 peerConfigs,
                                                 MemcachedConnection(_, _),
                                                 reSizer = Some(DefaultResizer(lowerBound = 5, upperBound = 15)))
}
