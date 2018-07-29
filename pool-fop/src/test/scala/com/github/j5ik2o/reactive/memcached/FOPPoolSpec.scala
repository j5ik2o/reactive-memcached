package com.github.j5ik2o.reactive.memcached

import cats.data.NonEmptyList
import monix.eval.Task

class FOPPoolSpec extends AbstractMemcachedConnectionPoolSpec("FOPPoolSpec") {

  override protected def createConnectionPool(
      connectionConfigs: NonEmptyList[PeerConfig]
  ): MemcachedConnectionPool[Task] =
    FOPPool.ofMultiple(FOPConfig(), connectionConfigs, MemcachedConnection(_, _))

}
