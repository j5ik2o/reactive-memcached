package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

final case class VersionRequest(id: UUID) extends CommandRequest with StringParsersSupport {

  override val key: String = UUID.randomUUID().toString

  override type Response = VersionResponse

  override val isMasterOnly: Boolean = true

  override def asString: String = "version"

  override protected def responseParser: P[Expr] = P(StringParsers.versionCommandResponse)

  override protected def parseResponse: Handler = {
    case (VersionExpr(value), next) =>
      (VersionSucceeded(UUID.randomUUID(), id, value), next)
    case (ErrorExpr, next) =>
      (VersionFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (VersionFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (VersionFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }
}

sealed trait VersionResponse                                                        extends CommandResponse
final case class VersionSucceeded(id: UUID, requestId: UUID, value: String)         extends VersionResponse
final case class VersionFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends VersionResponse
