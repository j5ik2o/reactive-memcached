package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import cats.Show
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.model._

import scala.concurrent.duration.Duration

final class AppendRequest private (val id: UUID,
                                   val key: String,
                                   val value: String,
                                   val expireDuration: Duration,
                                   val flags: Int,
                                   val noReply: Boolean = false)
    extends StorageRequest(id, key, flags, expireDuration, value, noReply) {

  override protected val commandName: String = "append"

  override type Response = AppendResponse

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (AppendSucceeded(UUID.randomUUID(), id), next)
    case (NotStoredExpr, next) =>
      (AppendNotStored(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (AppendNotFounded(UUID.randomUUID(), id), next)
    case (ExistsExpr, next) =>
      (AppendExisted(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (AppendFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (AppendFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (AppendFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }

}

object AppendRequest {

  def apply[A](id: UUID,
               key: String,
               value: A,
               expireDuration: Duration = Duration.Inf,
               flags: Int = 0,
               noReply: Boolean = false)(
      implicit s: Show[A]
  ): AppendRequest = new AppendRequest(id, key, s.show(value), expireDuration, flags, noReply)

}

sealed trait AppendResponse                                                        extends CommandResponse
final case class AppendNotStored(id: UUID, requestId: UUID)                        extends AppendResponse
final case class AppendNotFounded(id: UUID, requestId: UUID)                       extends AppendResponse
final case class AppendExisted(id: UUID, requestId: UUID)                          extends AppendResponse
final case class AppendSucceeded(id: UUID, requestId: UUID)                        extends AppendResponse
final case class AppendFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends AppendResponse
