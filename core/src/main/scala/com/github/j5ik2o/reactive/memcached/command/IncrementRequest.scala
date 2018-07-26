package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

final case class IncrementRequest(id: UUID, key: String, value: Long) extends CommandRequest with StringParsersSupport {

  override type Response = IncrementResponse
  override val isMasterOnly: Boolean = true

  override def asString: String = s"incr $key ${java.lang.Long.toUnsignedString(value)}"

  override protected def responseParser: P[Expr] = P(incOrDecCommandResponse)

  override protected def parseResponse: Handler = {
    case (NotFoundExpr, next) =>
      (IncrementNotFound(UUID.randomUUID(), id), next)
    case (StringExpr(s), next) =>
      (IncrementSucceeded(UUID.randomUUID(), id, java.lang.Long.parseUnsignedLong(s)), next)
    case (ErrorExpr, next) =>
      (IncrementFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (IncrementFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (IncrementFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }
}

sealed trait IncrementResponse                                                        extends CommandResponse
final case class IncrementNotFound(id: UUID, requestId: UUID)                         extends IncrementResponse
final case class IncrementSucceeded(id: UUID, requestId: UUID, value: Long)           extends IncrementResponse
final case class IncrementFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends IncrementResponse
