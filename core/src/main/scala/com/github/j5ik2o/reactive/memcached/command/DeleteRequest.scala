package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.StringParsers._
import com.github.j5ik2o.reactive.memcached.parser.model._
import fastparse.all._

final case class DeleteRequest(id: UUID, key: String, noReply: Boolean = false)
    extends CommandRequest
    with StringParsersSupport {

  override type Response = DeleteResponse
  override val isMasterOnly: Boolean = true

  override def asString: String = s"delete $key" + (if (noReply) " noreply" else "") + "\r\n"

  override protected def responseParser: P[Expr] =
    if (noReply) P(End).map(_ => EmptyExpr) else P(deletionCommandResponse)

  override protected def parseResponse: Handler = {
    case (EmptyExpr, next) =>
      (DeleteSucceeded(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (DeleteNotFound(UUID.randomUUID(), id), next)
    case (DeletedExpr, next) =>
      (DeleteSucceeded(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (DeleteFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (DeleteFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (DeleteFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

sealed trait DeleteResponse                                                        extends CommandResponse
final case class DeleteNotFound(id: UUID, requestId: UUID)                         extends DeleteResponse
final case class DeleteSucceeded(id: UUID, requestId: UUID)                        extends DeleteResponse
final case class DeleteFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends DeleteResponse
