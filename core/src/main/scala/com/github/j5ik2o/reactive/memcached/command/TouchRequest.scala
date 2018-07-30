package com.github.j5ik2o.reactive.memcached.command
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._

import scala.concurrent.duration._
import fastparse.all._

final case class TouchRequest(id: UUID, key: String, expireDuration: Duration)
    extends CommandRequest
    with StringParsersSupport {

  require(expireDuration.gt(1 seconds))

  override type Response = TouchResponse
  override val isMasterOnly: Boolean = true

  private def toSeconds: Long = if (expireDuration.isFinite()) expireDuration.toSeconds else 0

  override def asString: String = s"touch $key $toSeconds"

  override protected def responseParser: P[Expr] = P(touchCommandResponse)

  override protected def parseResponse: Handler = {
    case (NotFoundExpr, next) =>
      (TouchNotFounded(UUID.randomUUID(), id), next)
    case (TouchedExpr, next) =>
      (TouchSucceeded(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (TouchFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (TouchFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (TouchFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }
}

sealed trait TouchResponse                                                        extends CommandResponse
final case class TouchNotFounded(id: UUID, requestId: UUID)                       extends TouchResponse
final case class TouchSucceeded(id: UUID, requestId: UUID)                        extends TouchResponse
final case class TouchFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends TouchResponse
