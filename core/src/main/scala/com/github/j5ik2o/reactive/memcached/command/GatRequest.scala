package com.github.j5ik2o.reactive.memcached.command
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers.retrievalCommandResponse
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

import scala.concurrent.duration._

final class GatRequest private (val id: UUID, val key: String, val expireDuration: Duration)
    extends CommandRequest
    with StringParsersSupport {

  require(expireDuration.gt(1 seconds))

  override type Response = GatResponse
  override val isMasterOnly: Boolean = false

  private def toSeconds: Long = if (expireDuration.isFinite()) expireDuration.toSeconds else 0

  override def asString: String = s"gat $toSeconds $key"

  override protected def responseParser: P[Expr] = P(retrievalCommandResponse)

  override protected def parseResponse: Handler = {
    case (EndExpr, next) =>
      (GatSucceeded(UUID.randomUUID(), id, None), next)
    case (ValueExpr(key, flags, length, casUnique, value), next) =>
      (GatSucceeded(UUID.randomUUID(), id, Some(ValueDesc(key, flags, length, casUnique, value))), next)
    case (ErrorExpr, next) =>
      (GatFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (GatFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (GatFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object GatRequest {

  def apply(id: UUID, key: String, expireDuration: Duration): GatRequest = new GatRequest(id, key, expireDuration)

}

sealed trait GatResponse                                                           extends CommandResponse
final case class GatSucceeded(id: UUID, requestId: UUID, value: Option[ValueDesc]) extends GatResponse
final case class GatFailed(id: UUID, requestId: UUID, ex: MemcachedIOException)    extends GatResponse
