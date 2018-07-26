package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.model._

import scala.concurrent.duration.Duration

final case class PrependRequest(id: UUID,
                                key: String,
                                flags: Int,
                                expireDuration: Duration,
                                value: String,
                                noReply: Boolean = false)
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

sealed trait PrependResponse                                                        extends CommandResponse
final case class PrependNotStored(id: UUID, requestId: UUID)                        extends PrependResponse
final case class PrependNotFounded(id: UUID, requestId: UUID)                       extends PrependResponse
final case class PrependExisted(id: UUID, requestId: UUID)                          extends PrependResponse
final case class PrependSucceeded(id: UUID, requestId: UUID)                        extends PrependResponse
final case class PrependFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends PrependResponse
