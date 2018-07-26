package com.github.j5ik2o.reactive.memcached

import scala.concurrent.duration.Duration

final case class ScalaPoolConfig(sizePerPeer: Option[Int] = None,
                                 maxIdleTime: Option[Duration] = None,
                                 validationTimeout: Option[Duration] = None)
