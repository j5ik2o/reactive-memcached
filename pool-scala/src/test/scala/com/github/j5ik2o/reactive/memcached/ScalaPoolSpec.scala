package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

class ScalaPoolSpec extends AbstractMemcachedConnectionPoolSpec("ScalaPoolSpec") {

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    ScalaPool.ofMultiple(ScalaPoolConfig(), connectionConfigs, MemcachedConnection(_, _))

}
