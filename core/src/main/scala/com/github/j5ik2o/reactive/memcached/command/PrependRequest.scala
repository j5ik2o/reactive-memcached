package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.Show
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.model._

import scala.concurrent.duration.Duration

final class PrependRequest(override val id: UUID,
                           override val key: String,
                           override val value: String,
                           override val expireDuration: Duration,
                           override val flags: Int,
                           override val noReply: Boolean)
    extends StorageRequest(id, key, flags, expireDuration, value, noReply) {

  override protected val commandName: String = "prepend"

  override type Response = PrependResponse

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (PrependSucceeded(UUID.randomUUID(), id), next)
    case (NotStoredExpr, next) =>
      (PrependNotStored(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (PrependNotFounded(UUID.randomUUID(), id), next)
    case (ExistsExpr, next) =>
      (PrependExisted(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (PrependFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (PrependFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (PrependFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object PrependRequest {

  def apply[A](id: UUID, key: String, value: A, expireDuration: Duration, flags: Int, noReply: Boolean = false)(
      implicit s: Show[A]
  ): PrependRequest = new PrependRequest(id, key, s.show(value), expireDuration, flags, noReply)

}

sealed trait PrependResponse                                                        extends CommandResponse
final case class PrependNotStored(id: UUID, requestId: UUID)                        extends PrependResponse
final case class PrependNotFounded(id: UUID, requestId: UUID)                       extends PrependResponse
final case class PrependExisted(id: UUID, requestId: UUID)                          extends PrependResponse
final case class PrependSucceeded(id: UUID, requestId: UUID)                        extends PrependResponse
final case class PrependFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends PrependResponse
