package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

import scala.concurrent.duration.FiniteDuration

final case class SetRequest(id: UUID, key: String, flags: Int, expireDuration: FiniteDuration, value: String)
    extends CommandRequest
    with StringParsersSupport {

  override type Response = SetResponse
  override val isMasterOnly: Boolean = true

  override def asString: String = s"set $key $flags ${expireDuration.toSeconds} ${value.length}\r\n$value"

  override protected def responseParser: P[Expr] = P(storageCommandResponse)

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
