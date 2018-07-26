package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

class StormpotPoolSpec extends AbstractMemcachedConnectionPoolSpec("StormpotPoolSpec") {

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    StormpotPool.ofMultiple(StormpotConfig(), connectionConfigs, MemcachedConnection(_, _))

}
