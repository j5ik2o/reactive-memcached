package com.github.j5ik2o.reactive.memcached

import java.net.InetSocketAddress

import akka.io.Inet.SocketOption
import akka.stream.OverflowStrategy

import scala.collection.immutable
import scala.concurrent.duration._

final case class PeerConfig(remoteAddress: InetSocketAddress,
                            localAddress: Option[InetSocketAddress] = None,
                            options: immutable.Seq[SocketOption] = immutable.Seq.empty,
                            halfClose: Boolean = false,
                            connectTimeout: Duration = Duration.Inf,
                            idleTimeout: Duration = Duration.Inf,
                            backoffConfig: BackoffConfig = BackoffConfig(),
                            requestBufferSize: Int = PeerConfig.DEFAULT_MAX_REQUEST_BUFFER_SIZE,
                            overflowStrategy: OverflowStrategy = OverflowStrategy.backpressure)

object PeerConfig {
  val DEFAULT_MAX_REQUEST_BUFFER_SIZE: Int = 1024
}
