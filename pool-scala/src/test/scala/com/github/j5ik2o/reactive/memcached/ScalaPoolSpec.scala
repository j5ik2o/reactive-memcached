package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task
import monix.execution.Scheduler

class ScalaPoolSpec extends AbstractMemcachedConnectionPoolSpec("ScalaPoolSpec") {

  implicit val scheduler = Scheduler(system.dispatcher)

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    ScalaPool.ofMultiple(ScalaPoolConfig(), connectionConfigs, MemcachedConnection(_, _))

}
