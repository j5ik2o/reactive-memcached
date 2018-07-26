package com.github.j5ik2o.reactive.memcached
import java.io.PrintWriter

import org.apache.commons.pool2.impl.EvictionPolicy

import scala.concurrent.duration.Duration

final case class CommonsAbandonedConfig(removeAbandonedOnBorrow: Option[Boolean] = None,
                                        removeAbandonedOnMaintenance: Option[Boolean] = None,
                                        removeAbandonedTimeout: Option[Duration] = None,
                                        logAbandoned: Option[Boolean] = None,
                                        requireFullStackTrace: Option[Boolean] = None,
                                        logWriter: Option[PrintWriter] = None,
                                        useUsageTracking: Option[Boolean] = None)
final case class CommonsPoolConfig(
    lifo: Option[Boolean] = None,
    fairness: Option[Boolean] = None,
    maxWaitMillis: Option[Duration] = None,
    minEvictableIdleTime: Option[Duration] = None,
    evictorShutdownTimeout: Option[Duration] = None,
    softMinEvictableIdleTime: Option[Duration] = None,
    blockWhenExhausted: Option[Boolean] = None,
    evictionPolicy: Option[EvictionPolicy[MemcachedConnectionPoolable]] = None,
    evictionPolicyClassName: Option[String] = None,
    testOnCreate: Option[Boolean] = None,
    testOnBorrow: Option[Boolean] = None,
    testOnReturn: Option[Boolean] = None,
    testWhileIdle: Option[Boolean] = None,
    numTestsPerEvictionRun: Option[Int] = None,
    timeBetweenEvictionRuns: Option[Duration] = None,
    jmxEnabled: Option[Boolean] = None,
    jmxNamePrefix: Option[String] = None,
    jmxNameBase: Option[String] = None,
    sizePerPeer: Option[Int] = None,
    maxIdlePerPeer: Option[Int] = None,
    minIdlePerPeer: Option[Int] = None,
    abandonedConfig: Option[CommonsAbandonedConfig] = None
)
