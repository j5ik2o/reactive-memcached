package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.data.NonEmptyList
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import fastparse.all._

final class GetRequest private (val id: UUID, val key: String) extends CommandRequest with StringParsersSupport {

  override type Response = GetResponse
  override val isMasterOnly: Boolean = false

  override def asString: String = s"get $key"

  override protected def responseParser: P[Expr] = P(retrievalCommandResponse)

  override protected def parseResponse: Handler = {
    case (EndExpr, next) =>
      (GetSucceeded(UUID.randomUUID(), id, None), next)
    case (ValueExpr(key, flags, length, casUnique, value), next) =>
      (GetSucceeded(UUID.randomUUID(), id, Some(ValueDesc(key, flags, length, casUnique, value))), next)
    case (ErrorExpr, next) =>
      (GetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (GetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (GetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object GetRequest {

  def apply(id: UUID, key: String): GetRequest = new GetRequest(id, key)

}

final case class ValueDesc(key: String, flags: Int, length: Long, casUnique: Option[Long], value: String)

sealed trait GetResponse                                                           extends CommandResponse
final case class GetSucceeded(id: UUID, requestId: UUID, value: Option[ValueDesc]) extends GetResponse
final case class GetFailed(id: UUID, requestId: UUID, ex: MemcachedIOException)    extends GetResponse
