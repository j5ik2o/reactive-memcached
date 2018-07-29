package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task

class CommonsPoolSpec extends AbstractMemcachedConnectionPoolSpec("CommonsPoolSpec") {

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    CommonsPool.ofMultiple(CommonsPoolConfig(), connectionConfigs, MemcachedConnection(_, _))
}
