package com.github.j5ik2o.reactive.memcached.command

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import fastparse.all._

import scala.concurrent.duration.Duration

final case class SetRequest(id: UUID,
                            key: String,
                            flags: Int,
                            expireDuration: Duration,
                            value: String,
                            noReply: Boolean = false)
    extends CommandRequest
    with StringParsersSupport {

  override type Response = SetResponse
  override val isMasterOnly: Boolean = true

  private def expireDurationToSeconds: Long = if (expireDuration.isFinite()) expireDuration.toSeconds else 0
  private def bytes: Int                    = value.getBytes(StandardCharsets.UTF_8).length

  override def asString: String = s"set $key $flags $expireDurationToSeconds $bytes\r\n$value"

  override protected def responseParser: P[Expr] =
    if (noReply) P(End).map(_ => EmptyExpr) else P(storageCommandResponse)

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (SetSucceeded(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (SetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (SetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (SetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

sealed trait SetResponse                                                        extends CommandResponse
final case class SetSucceeded(id: UUID, requestId: UUID)                        extends SetResponse
final case class SetFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends SetResponse
