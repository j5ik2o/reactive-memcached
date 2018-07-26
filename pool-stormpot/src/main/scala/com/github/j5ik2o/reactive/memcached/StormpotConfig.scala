package com.github.j5ik2o.reactive.memcached

import com.github.j5ik2o.reactive.memcached.PoolType.Queue

import scala.concurrent.duration.{ Duration, FiniteDuration }

final case class StormpotConfig(poolType: PoolType = Queue,
                                sizePerPeer: Option[Int] = None,
                                claimTimeout: Option[FiniteDuration] = None,
                                backgroundExpirationEnabled: Option[Boolean] = None,
                                preciseLeakDetectionEnabled: Option[Boolean] = None,
                                validationTimeout: Option[Duration] = None)
