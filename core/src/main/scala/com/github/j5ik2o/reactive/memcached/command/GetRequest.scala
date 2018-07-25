package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.data.NonEmptyList
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import fastparse.all._

case class GetRequest(id: UUID, key: NonEmptyList[String]) extends CommandRequest with StringParsersSupport {

  override type Response = GetResponse
  override val isMasterOnly: Boolean = false

  override def asString: String = s"get $key"

  override protected def responseParser: P[Expr] = P(retrievalCommandResponse)

  override protected def parseResponse: Handler = {
    case (EndExpr, next) =>
      (GetSucceeded(UUID.randomUUID(), id, None), next)
    case (ValueExpr(key, flags, expireTime, value), next) =>
      (GetSucceeded(UUID.randomUUID(), id, Some(ValueDesc(key, flags, expireTime, value))), next)
    case (ErrorExpr, next) =>
      (GetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (GetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (GetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

case class ValueDesc(key: String, flags: Int, expire: Long, value: String)

sealed trait GetResponse                                                     extends CommandResponse
case class GetSucceeded(id: UUID, requestId: UUID, value: Option[ValueDesc]) extends GetResponse
case class GetFailed(id: UUID, requestId: UUID, ex: MemcachedIOException)    extends GetResponse
