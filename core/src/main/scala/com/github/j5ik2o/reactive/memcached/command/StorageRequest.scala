package com.github.j5ik2o.reactive.memcached.command

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.parser.StringParsers.storageCommandResponse
import com.github.j5ik2o.reactive.memcached.parser.model.{ EmptyExpr, Expr }
import fastparse.all._

import scala.concurrent.duration._

abstract class StorageRequest(val id: UUID,
                              val key: String,
                              val flags: Int,
                              val expireDuration: Duration,
                              val value: String,
                              val noReply: Boolean = false)
    extends CommandRequest
    with StringParsersSupport {

  require(expireDuration.gt(1 seconds))

  protected val commandName: String
  override val isMasterOnly: Boolean    = true
  protected val casUnique: Option[Long] = None
  protected lazy val isCas: Boolean     = casUnique.nonEmpty

  private def toSeconds: Long = if (expireDuration.isFinite()) expireDuration.toSeconds else 0
  private def bytes: Int      = value.getBytes(StandardCharsets.UTF_8).length

  override def asString: String =
    s"${commandName.toLowerCase} $key $flags $toSeconds $bytes${casUnique.fold("")(v => s" $v")}\r\n$value"

  override protected def responseParser: P[Expr] =
    if (noReply) P(End).map(_ => EmptyExpr) else P(storageCommandResponse)

}
