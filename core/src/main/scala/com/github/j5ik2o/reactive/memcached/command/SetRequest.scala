package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.parser.model._
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }

import scala.concurrent.duration.Duration

final case class SetRequest(id: UUID,
                            key: String,
                            flags: Int,
                            expireDuration: Duration,
                            value: String,
                            noReply: Boolean = false)
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

sealed trait SetResponse                                                        extends CommandResponse
final case class SetNotStored(id: UUID, requestId: UUID)                        extends SetResponse
final case class SetNotFounded(id: UUID, requestId: UUID)                       extends SetResponse
final case class SetExisted(id: UUID, requestId: UUID)                          extends SetResponse
final case class SetSucceeded(id: UUID, requestId: UUID)                        extends SetResponse
final case class SetFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends SetResponse
