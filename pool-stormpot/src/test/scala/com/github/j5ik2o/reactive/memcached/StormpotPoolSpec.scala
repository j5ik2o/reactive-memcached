package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler

class StormpotPoolSpec extends AbstractMemcachedConnectionPoolSpec("StormpotPoolSpec") {

  implicit val scheduler = Scheduler(system.dispatcher)

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    StormpotPool.ofMultiple(StormpotConfig(), connectionConfigs, MemcachedConnection(_, _))

}
