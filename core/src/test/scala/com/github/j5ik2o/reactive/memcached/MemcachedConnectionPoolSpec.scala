package com.github.j5ik2o.reactive.memcached
import akka.routing.DefaultResizer
import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler

class MemcachedConnectionPoolSpec extends AbstractMemcachedConnectionPoolSpec("MemcachedConnectionPoolSpec") {
  implicit val scheduler = Scheduler(system.dispatcher)
  override protected def createConnectionPool(
      peerConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    MemcachedConnectionPool.ofMultipleRoundRobin(sizePerPeer = 10,
                                                 peerConfigs,
                                                 MemcachedConnection(_, _),
                                                 reSizer = Some(DefaultResizer(lowerBound = 5, upperBound = 15)))
}
