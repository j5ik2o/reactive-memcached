package com.github.j5ik2o.reactive.memcached

import scala.concurrent.duration.FiniteDuration

final case class FOPConfig(maxSizePerPeer: Option[Int] = None,
                           minSizePerPeer: Option[Int] = None,
                           maxWaitDuration: Option[FiniteDuration] = None,
                           maxIdleDuration: Option[FiniteDuration] = None,
                           partitionSizePerPeer: Option[Int] = None,
                           scavengeInterval: Option[FiniteDuration] = None,
                           scavengeRatio: Option[Double] = None,
                           validationTimeout: Option[FiniteDuration] = None)
