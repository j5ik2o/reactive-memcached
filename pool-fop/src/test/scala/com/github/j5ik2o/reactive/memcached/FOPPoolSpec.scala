package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler

class FOPPoolSpec extends AbstractMemcachedConnectionPoolSpec("FOPPoolSpec") {

  implicit val scheduler = Scheduler(system.dispatcher)

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    FOPPool.ofMultiple(FOPConfig(), connectionConfigs, MemcachedConnection(_, _))

}
