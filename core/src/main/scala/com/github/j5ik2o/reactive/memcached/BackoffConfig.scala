package com.github.j5ik2o.reactive.memcached

import scala.concurrent.duration._

final case class BackoffConfig(minBackoff: FiniteDuration = 3 seconds,
                               maxBackoff: FiniteDuration = 30 seconds,
                               randomFactor: Double = 0.2,
                               maxRestarts: Int = -1)
