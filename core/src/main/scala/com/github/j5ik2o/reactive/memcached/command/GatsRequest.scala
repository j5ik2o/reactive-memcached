package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers.retrievalCommandResponse
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

import scala.concurrent.duration._

final class GatsRequest private (val id: UUID, val key: String, val expireDuration: Duration)
    extends CommandRequest
    with StringParsersSupport {
  require(expireDuration.gt(1 seconds))

  override type Response = GatsResponse
  override val isMasterOnly: Boolean = false

  private def toSeconds: Long = if (expireDuration.isFinite()) expireDuration.toSeconds else 0

  override def asString: String = s"gats $toSeconds $key"

  override protected def responseParser: P[Expr] = P(retrievalCommandResponse)

  override protected def parseResponse: Handler = {
    case (EndExpr, next) =>
      (GatsSucceeded(UUID.randomUUID(), id, None), next)
    case (ValueExpr(key, flags, length, casUnique, value), next) =>
      (GatsSucceeded(UUID.randomUUID(), id, Some(ValueDesc(key, flags, length, casUnique, value))), next)
    case (ErrorExpr, next) =>
      (GatsFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (GatsFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (GatsFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object GatsRequest {

  def apply(id: UUID, key: String, expireDuration: Duration): GatsRequest = new GatsRequest(id, key, expireDuration)

}

sealed trait GatsResponse                                                           extends CommandResponse
final case class GatsSucceeded(id: UUID, requestId: UUID, value: Option[ValueDesc]) extends GatsResponse
final case class GatsFailed(id: UUID, requestId: UUID, ex: MemcachedIOException)    extends GatsResponse
