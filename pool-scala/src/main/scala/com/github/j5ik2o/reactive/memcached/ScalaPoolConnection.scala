package com.github.j5ik2o.reactive.memcached

import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.reactive.memcached.command.CommandRequest
import io.github.andrebeat.pool.Lease
import monix.eval.Task
import monix.execution.Scheduler

private[memcached] final case class ScalaPoolConnection(underlying: Lease[MemcachedConnection])
    extends MemcachedConnection {

  private val underlyingCon = underlying.get()

  override def id: UUID = underlyingCon.id

  override def peerConfig: Option[PeerConfig] = underlyingCon.peerConfig

  override def shutdown(): Unit = underlyingCon.shutdown()

  override def toFlow[C <: CommandRequest](parallelism: Int)(
      implicit scheduler: Scheduler
  ): Flow[C, C#Response, NotUsed] = underlyingCon.toFlow(parallelism)

  override def send[C <: CommandRequest](cmd: C): Task[cmd.Response] = underlyingCon.send(cmd)

}
