package com.github.j5ik2o.reactive.memcached.command
import java.util.UUID

import cats.Show
import com.github.j5ik2o.reactive.memcached.{ ErrorType, MemcachedIOException }
import com.github.j5ik2o.reactive.memcached.parser.model._

import scala.concurrent.duration.Duration

final class CasRequest private (override val id: UUID,
                                override val key: String,
                                override val value: String,
                                val _casUnique: Long,
                                override val expireDuration: Duration,
                                override val flags: Int,
                                override val noReply: Boolean)
    extends StorageRequest(id, key, flags, expireDuration, value, noReply) {
  override type Response = CasResponse
  override protected val commandName: String = "cas"

  override protected val casUnique: Option[Long] = Some(_casUnique)

  override protected def parseResponse: Handler = {
    case (StoredExpr, next) =>
      (CasSucceeded(UUID.randomUUID(), id), next)
    case (NotStoredExpr, next) =>
      (CasNotStored(UUID.randomUUID(), id), next)
    case (NotFoundExpr, next) =>
      (CasNotFounded(UUID.randomUUID(), id), next)
    case (ExistsExpr, next) =>
      (CasExisted(UUID.randomUUID(), id), next)
    case (ErrorExpr, next) =>
      (CasFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.OtherType, None)), next)
    case (ClientErrorExpr(msg), next) =>
      (CasFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ClientType, Some(msg))), next)
    case (ServerErrorExpr(msg), next) =>
      (CasFailed(UUID.randomUUID(), id, MemcachedIOException(ErrorType.ServerType, Some(msg))), next)
  }
}

object CasRequest {

  def apply[A](id: UUID,
               key: String,
               value: A,
               casUnique: Long,
               expireDuration: Duration,
               flags: Int,
               noReply: Boolean = false)(implicit s: Show[A]): CasRequest =
    new CasRequest(id, key, s.show(value), casUnique, expireDuration, flags, noReply)

}

sealed trait CasResponse                                                        extends CommandResponse
final case class CasSucceeded(id: UUID, requestId: UUID)                        extends CasResponse
final case class CasNotStored(id: UUID, requestId: UUID)                        extends CasResponse
final case class CasNotFounded(id: UUID, requestId: UUID)                       extends CasResponse
final case class CasExisted(id: UUID, requestId: UUID)                          extends CasResponse
final case class CasFailed(id: UUID, requestId: UUID, ex: MemcachedIOException) extends CasResponse
