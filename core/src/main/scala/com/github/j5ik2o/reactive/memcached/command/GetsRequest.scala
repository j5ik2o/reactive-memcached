package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.data.NonEmptyList
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import fastparse.all._

final class GetsRequest private (val id: UUID, val key: String) extends CommandRequest with StringParsersSupport {

  override type Response = GetsResponse
  override val isMasterOnly: Boolean = false

  override def asString: String = s"gets $key"

  override protected def responseParser: P[Expr] = P(retrievalCommandResponse)

  override protected def parseResponse: Handler = {
    case (EndExpr, next) =>
      (GetsSucceeded(UUID.randomUUID(), id, None), next)
    case (ValueExpr(key, flags, length, casUnique, value), next) =>
      (GetsSucceeded(UUID.randomUUID(), id, Some(ValueDesc(key, flags, length, casUnique, value))), next)
    case (ErrorExpr, next) =>
      (GetsFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (GetsFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (GetsFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object GetsRequest {

  def apply(id: UUID, key: String): GetsRequest = new GetsRequest(id, key)

}

sealed trait GetsResponse                                                           extends CommandResponse
final case class GetsSucceeded(id: UUID, requestId: UUID, value: Option[ValueDesc]) extends GetsResponse
final case class GetsFailed(id: UUID, requestId: UUID, ex: MemcachedIOException)    extends GetsResponse
