package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.Show
import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }

import scala.concurrent.duration.Duration

final class SetRequest private (val id: UUID,
                                val key: String,
                                val value: String,
                                val expireDuration: Duration,
                                val flags: Int,
                                val noReply: Boolean = false)
    extends StorageRequest(id, key, flags, expireDuration, value, noReply) {

  override val commandName = "set"
  override type Response = SetResponse

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (SetSucceeded(UUID.randomUUID(), id), next)
    case (NotStoredExpr, next) =>
      (SetNotStored(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (SetNotFounded(UUID.randomUUID(), id), next)
    case (ExistsExpr, next) =>
      (SetExisted(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (SetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (SetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (SetFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object SetRequest {

  def apply[A](id: UUID,
               key: String,
               value: A,
               expireDuration: Duration = Duration.Inf,
               flags: Int = 0,
               noReply: Boolean = false)(implicit s: Show[A]): SetRequest =
    new SetRequest(id, key, s.show(value), expireDuration, flags, noReply)
}

sealed trait SetResponse                                                        extends CommandResponse
final case class SetNotStored(id: UUID, requestId: UUID)                        extends SetResponse
final case class SetNotFounded(id: UUID, requestId: UUID)                       extends SetResponse
final case class SetExisted(id: UUID, requestId: UUID)                          extends SetResponse
final case class SetSucceeded(id: UUID, requestId: UUID)                        extends SetResponse
final case class SetFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends SetResponse
