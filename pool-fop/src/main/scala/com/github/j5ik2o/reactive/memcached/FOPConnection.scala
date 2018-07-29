package com.github.j5ik2o.reactive.memcached

import java.util.UUID

import cn.danielw.fop.Poolable
import com.github.j5ik2o.reactive.memcached.command.CommandRequest
import monix.eval.Task

final case class FOPConnection(underlying: Poolable[MemcachedConnection]) extends MemcachedConnection {
  private val underlyingCon = underlying.getObject

  override def id: UUID = underlyingCon.id

  override def peerConfig: Option[PeerConfig] = underlyingCon.peerConfig

  override def shutdown(): Unit = underlyingCon.shutdown()

  override def send[C <: CommandRequest](cmd: C): Task[cmd.Response] = underlyingCon.send(cmd)

}
