package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.Show
import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }

import scala.concurrent.duration.Duration

final case class ReplaceRequest(override val id: UUID,
                                override val key: String,
                                override val value: String,
                                override val expireDuration: Duration,
                                override val flags: Int,
                                override val noReply: Boolean)
    extends StorageRequest(id, key, flags, expireDuration, value, noReply) {

  override protected val commandName: String = "replace"

  override type Response = ReplaceResponse

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (ReplaceSucceeded(UUID.randomUUID(), id), next)
    case (NotStoredExpr, next) =>
      (ReplaceNotStored(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (ReplaceNotFounded(UUID.randomUUID(), id), next)
    case (ExistsExpr, next) =>
      (ReplaceExisted(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (ReplaceFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (ReplaceFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (ReplaceFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object ReplaceRequest {

  def apply[A](id: UUID,
               key: String,
               value: A,
               expireDuration: Duration = Duration.Inf,
               flags: Int = 0,
               noReply: Boolean = false)(
      implicit s: Show[A]
  ): ReplaceRequest = new ReplaceRequest(id, key, s.show(value), expireDuration, flags, noReply)

}

sealed trait ReplaceResponse                                                        extends CommandResponse
final case class ReplaceNotStored(id: UUID, requestId: UUID)                        extends ReplaceResponse
final case class ReplaceNotFounded(id: UUID, requestId: UUID)                       extends ReplaceResponse
final case class ReplaceExisted(id: UUID, requestId: UUID)                          extends ReplaceResponse
final case class ReplaceSucceeded(id: UUID, requestId: UUID)                        extends ReplaceResponse
final case class ReplaceFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends ReplaceResponse
