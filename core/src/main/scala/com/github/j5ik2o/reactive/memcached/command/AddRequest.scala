package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.Show
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.model._

import scala.concurrent.duration.Duration

final case class AddRequest private (override val id: UUID,
                                     override val key: String,
                                     override val value: String,
                                     override val expireDuration: Duration,
                                     override val flags: Int,
                                     override val noReply: Boolean)
    extends StorageRequest(id, key, flags, expireDuration, value, noReply) {
  override protected val commandName: String = "add"

  override type Response = AddResponse

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (AddSucceeded(UUID.randomUUID(), id), next)
    case (NotStoredExpr, next) =>
      (AddNotStored(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (AddNotFounded(UUID.randomUUID(), id), next)
    case (ExistsExpr, next) =>
      (AddExisted(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (AddFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (AddFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (AddFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object AddRequest {

  def apply[A](id: UUID,
               key: String,
               value: A,
               expireDuration: Duration = Duration.Inf,
               flags: Int = 0,
               noReply: Boolean = false)(
      implicit s: Show[A]
  ): AddRequest = new AddRequest(id, key, s.show(value), expireDuration, flags, noReply)

}

sealed trait AddResponse                                                        extends CommandResponse
final case class AddNotStored(id: UUID, requestId: UUID)                        extends AddResponse
final case class AddNotFounded(id: UUID, requestId: UUID)                       extends AddResponse
final case class AddExisted(id: UUID, requestId: UUID)                          extends AddResponse
final case class AddSucceeded(id: UUID, requestId: UUID)                        extends AddResponse
final case class AddFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends AddResponse
