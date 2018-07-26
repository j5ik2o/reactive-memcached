package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

final case class DecrementRequest(id: UUID, key: String, value: Long) extends CommandRequest with StringParsersSupport {

  override type Response = DecrementResponse
  override val isMasterOnly: Boolean = true

  override def asString: String = s"decr $key ${java.lang.Long.toUnsignedString(value)}"

  override protected def responseParser: P[Expr] = P(incOrDecCommandResponse)

  override protected def parseResponse: Handler = {
    case (NotFoundExpr, next) =>
      (DecrementNotFound(UUID.randomUUID(), id), next)
    case (StringExpr(s), next) =>
      (DecrementSucceeded(UUID.randomUUID(), id, java.lang.Long.parseUnsignedLong(s)), next)
    case (ErrorExpr, next) =>
      (DecrementFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (DecrementFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (DecrementFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

sealed trait DecrementResponse                                                        extends CommandResponse
final case class DecrementNotFound(id: UUID, requestId: UUID)                         extends DecrementResponse
final case class DecrementSucceeded(id: UUID, requestId: UUID, value: Long)           extends DecrementResponse
final case class DecrementFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends DecrementResponse
