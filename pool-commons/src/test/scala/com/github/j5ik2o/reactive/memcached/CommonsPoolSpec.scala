package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler

class CommonsPoolSpec extends AbstractMemcachedConnectionPoolSpec("CommonsPoolSpec") {

  implicit val scheduler = Scheduler(system.dispatcher)

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    CommonsPool.ofMultiple(CommonsPoolConfig(), connectionConfigs, MemcachedConnection(_, _))
}
